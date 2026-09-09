import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import core.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class JavaLoadBalancer {
    private static List<Backend> backendList;
    private static LoadBalancingStrategy strategy;
    private static HttpClient httpClient;
    private static final DateTimeFormatter LOG_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final int MAX_RETRIES = 2; // Số lần thử lại tối đa sang backend khác nếu lỗi

    public static void main(String[] args) throws IOException {
        ConfigLoader.Config config = ConfigLoader.loadConfig("config.json");
        int port = config.getPort();

        backendList = config.getBackends().stream()
                .map(b -> new Backend(b.getUrl(), b.getWeight()))
                .collect(Collectors.toList());

        strategy = StrategyFactory.getStrategy(config.getAlgorithm());

        // Khởi tạo HTTP Client dùng chung với Connection Pool & Keep-Alive cho Java 17
        httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(2))
                .executor(Executors.newFixedThreadPool(50))
                .build();

        HealthChecker healthChecker = new HealthChecker(backendList);
        healthChecker.start(5);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new ProxyHandler());
        server.setExecutor(Executors.newFixedThreadPool(100));

        System.out.println("=================================================");
        System.out.println(">>> Load Balancer running at: http://localhost:" + port);
        System.out.println(">>> Engine: Java HttpClient (Keep-Alive + Pool)");
        System.out.println(">>> Algorithm: " + config.getAlgorithm());
        System.out.println(">>> Backends: " + backendList.size());
        System.out.println("=================================================");

        server.start();
    }

    static class ProxyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            long startTime = System.currentTimeMillis();
            String method = exchange.getRequestMethod();
            String uri = exchange.getRequestURI().toString();
            String clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();
            byte[] requestBody = exchange.getRequestBody().readAllBytes();

            Set<Backend> triedBackends = new HashSet<>();
            HttpResponse<byte[]> response = null;
            Backend selectedBackend = null;
            int attempts = 0;

            // Vòng lặp Retry: nếu backend bị lỗi mạng hoặc sập đột ngột, tự động chuyển sang backend khác
            while (attempts <= MAX_RETRIES) {
                // Lọc bỏ những backend đã thử và thất bại trong lượt request này
                List<Backend> availableBackends = backendList.stream()
                        .filter(b -> !triedBackends.contains(b))
                        .collect(Collectors.toList());

                selectedBackend = strategy.selectBackend(availableBackends, clientIp);

                if (selectedBackend == null) {
                    break;
                }

                triedBackends.add(selectedBackend);
                selectedBackend.incrementConnections();

                try {
                    URI targetUri = URI.create(selectedBackend.getUrl() + uri);

                    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                            .uri(targetUri)
                            .timeout(Duration.ofSeconds(3))
                            .header("X-Forwarded-For", clientIp);

                    // Forward headers từ client (trừ restricted headers)
                    for (String headerKey : exchange.getRequestHeaders().keySet()) {
                        if (!isRestrictedHeader(headerKey)) {
                            for (String val : exchange.getRequestHeaders().get(headerKey)) {
                                requestBuilder.header(headerKey, val);
                            }
                        }
                    }

                    // Đính kèm Body nếu có
                    if (requestBody.length > 0) {
                        requestBuilder.method(method, HttpRequest.BodyPublishers.ofByteArray(requestBody));
                    } else {
                        requestBuilder.method(method, HttpRequest.BodyPublishers.noBody());
                    }

                    response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofByteArray());

                    // Thành công nhận phản hồi từ backend
                    break;

                } catch (Exception ex) {
                    // Backend đột ngột timeout hoặc connection refused
                    selectedBackend.setAlive(false); // Tạm thời cô lập backend
                    System.err.printf("[WARN] Backend %s failed: %s. Dang thu lai backend khac...%n",
                            selectedBackend.getUrl(), ex.getMessage());
                    attempts++;
                } finally {
                    selectedBackend.decrementConnections();
                }
            }

            // Phản hồi về Client
            if (response != null) {
                // Copy headers từ backend response về client
                response.headers().map().forEach((key, values) -> {
                    if (!key.equalsIgnoreCase("Transfer-Encoding") && !key.equalsIgnoreCase("Content-Length")) {
                        for (String val : values) {
                            exchange.getResponseHeaders().add(key, val);
                        }
                    }
                });

                byte[] body = response.body();
                exchange.sendResponseHeaders(response.statusCode(), body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
                printLog(clientIp, method, uri, selectedBackend.getUrl(), response.statusCode(), System.currentTimeMillis() - startTime);

            } else {
                byte[] errorMsg = "503 Service Unavailable: Tat ca backend deu ban hoac gap su co".getBytes();
                exchange.sendResponseHeaders(503, errorMsg.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(errorMsg);
                }
                String backendUrl = selectedBackend != null ? selectedBackend.getUrl() : "NONE";
                printLog(clientIp, method, uri, backendUrl, 503, System.currentTimeMillis() - startTime);
            }
        }

        private boolean isRestrictedHeader(String header) {
            return header == null ||
                    header.equalsIgnoreCase("Host") ||
                    header.equalsIgnoreCase("Connection") ||
                    header.equalsIgnoreCase("Content-Length");
        }

        private void printLog(String clientIp, String method, String uri, String backendUrl, int statusCode, long duration) {
            String timestamp = LocalDateTime.now().format(LOG_DATE_FORMAT);
            System.out.printf("[%s] %s | %s %s -> %s | Status: %d | Duration: %d ms%n",
                    timestamp, clientIp, method, uri, backendUrl, statusCode, duration);
        }
    }
}
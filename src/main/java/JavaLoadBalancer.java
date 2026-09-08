import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class JavaLoadBalancer {

    private static final List<String> BACKENDS = List.of(
            "http://localhost:8081",
            "http://localhost:8082",
            "http://localhost:8083"
    );

    private static final AtomicInteger counter = new AtomicInteger(0);

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    // Danh sách header không được forward thủ công để tránh xung đột HTTP client/server
    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
            "host", "connection", "content-length", "transfer-encoding",
            "keep-alive", "proxy-authenticate", "proxy-authorization", "te", "trailers", "upgrade"
    );

    public static void main(String[] args) throws IOException {
        int lbPort = 8000;
        HttpServer server = HttpServer.create(new InetSocketAddress(lbPort), 0);

        server.createContext("/", new LoadBalancerHandler());
        server.setExecutor(Executors.newFixedThreadPool(50)); // Tận dụng Virtual Threads trên JDK mới

        System.out.println(">>> Java Load Balancer đang chạy tại: http://localhost:" + lbPort);
        server.start();
    }

    static class LoadBalancerHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 1. Chọn Backend theo Round Robin
            int index = Math.abs(counter.getAndIncrement() % BACKENDS.size());
            String targetHost = BACKENDS.get(index);
            String fullTargetUrl = targetHost + exchange.getRequestURI().toString();

            try {
                // 2. Chuẩn bị request gửi sang backend
                byte[] requestBody = exchange.getRequestBody().readAllBytes();
                HttpRequest.BodyPublisher bodyPublisher = requestBody.length > 0
                        ? HttpRequest.BodyPublishers.ofByteArray(requestBody)
                        : HttpRequest.BodyPublishers.noBody();

                HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(fullTargetUrl))
                        .method(exchange.getRequestMethod(), bodyPublisher)
                        .timeout(Duration.ofSeconds(5));

                exchange.getRequestHeaders().forEach((key, values) -> {
                    if (!HOP_BY_HOP_HEADERS.contains(key.toLowerCase())) {
                        for (String value : values) {
                            reqBuilder.header(key, value);
                        }
                    }
                });

                reqBuilder.header("X-Forwarded-For", exchange.getRemoteAddress().getAddress().getHostAddress());

                // 3. Nhận phản hồi từ Backend dưới dạng mảng byte
                HttpResponse<byte[]> response = httpClient.send(
                        reqBuilder.build(),
                        HttpResponse.BodyHandlers.ofByteArray()
                );

                byte[] responseBytes = response.body();

                // 4. Ghi header phản hồi về Client
                response.headers().map().forEach((key, values) -> {
                    if (!HOP_BY_HOP_HEADERS.contains(key.toLowerCase())) {
                        for (String value : values) {
                            exchange.getResponseHeaders().add(key, value);
                        }
                    }
                });

                // Xác định chính xác độ dài byte trả về
                exchange.sendResponseHeaders(response.statusCode(), responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                    os.flush();
                }

            } catch (Exception e) {
                e.printStackTrace();
                String errorMsg = "502 Bad Gateway: " + e.getMessage();
                byte[] errorBytes = errorMsg.getBytes();
                exchange.sendResponseHeaders(502, errorBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(errorBytes);
                    os.flush();
                }
            } finally {
                exchange.close();
            }
        }
    }
}
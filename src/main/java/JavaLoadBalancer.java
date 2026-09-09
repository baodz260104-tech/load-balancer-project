import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import core.Backend;
import core.ConfigLoader;
import core.HealthChecker;
import core.RoundRobinBalancer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class JavaLoadBalancer {
    private static RoundRobinBalancer balancer;
    private static final DateTimeFormatter LOG_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public static void main(String[] args) throws IOException {
        // 1. Doc file config.json
        ConfigLoader.Config config = ConfigLoader.loadConfig("config.json");
        int port = config.getPort();

        List<Backend> backendList = config.getBackends().stream()
                .map(Backend::new)
                .collect(Collectors.toList());

        balancer = new RoundRobinBalancer(backendList);

        // 2. Bat Health Checker chay ngam dinh ky moi 5 giay
        HealthChecker healthChecker = new HealthChecker(backendList);
        healthChecker.start(5);

        // 3. Khoi tao HTTP Server lang nghe tren cong duoc cau hinh
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new ProxyHandler());
        server.setExecutor(Executors.newFixedThreadPool(50));

        System.out.println("=================================================");
        System.out.println(">>> Load Balancer dang chay tai: http://localhost:" + port);
        System.out.println(">>> So luong backend duoc cau hinh: " + backendList.size());
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

            Backend targetBackend = balancer.getNextBackend();

            // Neu tat ca backend deu chet hoac khong co backend nao kha dung
            if (targetBackend == null) {
                byte[] response = "503 Service Unavailable: Tat ca backend deu offline".getBytes();
                exchange.sendResponseHeaders(503, response.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response);
                }
                long duration = System.currentTimeMillis() - startTime;
                printLog(clientIp, method, uri, "NONE", 503, duration);
                return;
            }

            targetBackend.incrementConnections();
            int responseCode = 502;

            try {
                // Forward request sang backend
                URL targetUrl = new URL(targetBackend.getUrl() + uri);
                HttpURLConnection conn = (HttpURLConnection) targetUrl.openConnection();
                conn.setRequestMethod(method);
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(5000);

                // Copy Request Headers
                for (String headerKey : exchange.getRequestHeaders().keySet()) {
                    if (headerKey != null && !headerKey.equalsIgnoreCase("Host")) {
                        conn.setRequestProperty(headerKey, exchange.getRequestHeaders().getFirst(headerKey));
                    }
                }
                conn.setRequestProperty("X-Forwarded-For", clientIp);

                // Copy Body neu la POST/PUT
                if (exchange.getRequestBody().available() > 0) {
                    conn.setDoOutput(true);
                    try (InputStream is = exchange.getRequestBody(); OutputStream os = conn.getOutputStream()) {
                        is.transferTo(os);
                    }
                }

                responseCode = conn.getResponseCode();

                // Copy Response Headers ve Client
                for (String key : conn.getHeaderFields().keySet()) {
                    if (key != null && !key.equalsIgnoreCase("Transfer-Encoding")) {
                        exchange.getResponseHeaders().set(key, conn.getHeaderField(key));
                    }
                }

                InputStream respStream = (responseCode >= 400) ? conn.getErrorStream() : conn.getInputStream();
                byte[] body = (respStream != null) ? respStream.readAllBytes() : new byte[0];

                exchange.sendResponseHeaders(responseCode, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }

            } catch (Exception e) {
                responseCode = 502;
                byte[] errorMsg = ("502 Bad Gateway: " + e.getMessage()).getBytes();
                exchange.sendResponseHeaders(502, errorMsg.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(errorMsg);
                }
            } finally {
                targetBackend.decrementConnections();
                long duration = System.currentTimeMillis() - startTime;
                printLog(clientIp, method, uri, targetBackend.getUrl(), responseCode, duration);
            }
        }

        private void printLog(String clientIp, String method, String uri, String backendUrl, int statusCode, long duration) {
            String timestamp = LocalDateTime.now().format(LOG_DATE_FORMAT);
            System.out.printf("[%s] %s | %s %s -> %s | Status: %d | Duration: %d ms%n",
                    timestamp, clientIp, method, uri, backendUrl, statusCode, duration);
        }
    }
}
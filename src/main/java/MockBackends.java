import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class MockBackends {
    public static void main(String[] args) throws IOException {
        int[] ports = {8081, 8082, 8083};

        for (int port : ports) {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

            // Xử lý request thông thường
            server.createContext("/", new BackendHandler(port));

            // Endpoint health check (phục vụ tuần 2)
            server.createContext("/health", (exchange) -> {
                String response = "OK";
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            });

            server.setExecutor(null);
            server.start();
            System.out.println("Backend Server đang chạy tại port: " + port);
        }
    }

    static class BackendHandler implements HttpHandler {
        private final int port;

        public BackendHandler(int port) {
            this.port = port;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String clientIp = exchange.getRequestHeaders().getFirst("X-Forwarded-For");
            String response = String.format("Phản hồi từ Backend port %d | Client IP: %s\n",
                    port, (clientIp != null ? clientIp : "Trực tiếp"));

            byte[] responseBytes = response.getBytes();
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }
}
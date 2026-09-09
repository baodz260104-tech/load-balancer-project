import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class MockBackends {
    public static void main(String[] args) throws IOException {
        int[] ports = {8081, 8082, 8083, 8084}; // Đảm bảo mở đủ 4 cổng

        for (int port : ports) {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

            // Xử lý request thông thường
            server.createContext("/", new EchoHandler(port));

            // Xử lý request Health Check của Load Balancer
            server.createContext("/health", exchange -> {
                String response = "OK";
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            });

            server.setExecutor(null);
            server.start();
            System.out.println("Mock Backend dang chay tai port: " + port);
        }
    }

    static class EchoHandler implements HttpHandler {
        private final int port;

        public EchoHandler(int port) {
            this.port = port;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "Phan hoi tu Backend cong " + port;
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
}
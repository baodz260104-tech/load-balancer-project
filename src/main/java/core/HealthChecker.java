package core;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HealthChecker {
    private final List<Backend> backends;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    public HealthChecker(List<Backend> backends) {
        this.backends = backends;
    }

    public void start(int intervalSeconds) {
        scheduler.scheduleAtFixedRate(this::checkAllBackends, 0, intervalSeconds, TimeUnit.SECONDS);
        System.out.println(">>> Health Checker da khoi dong (Chu ky: " + intervalSeconds + "s)");
    }

    private void checkAllBackends() {
        for (Backend backend : backends) {
            String healthUrl = backend.getUrl() + "/health";
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(healthUrl))
                        .timeout(Duration.ofSeconds(2))
                        .GET()
                        .build();

                HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
                boolean wasAlive = backend.isAlive();
                boolean isNowAlive = (response.statusCode() == 200);

                backend.setAlive(isNowAlive);
                if (!wasAlive && isNowAlive) {
                    System.out.println("[HEALTH] " + backend.getUrl() + " da PHUC HOI -> ONLINE");
                }
            } catch (Exception e) {
                if (backend.isAlive()) {
                    backend.setAlive(false);
                    System.err.println("[HEALTH] " + backend.getUrl() + " KHONG PHAN HOI -> OFFLINE");
                }
            }
        }
    }

    public void stop() {
        scheduler.shutdown();
    }
}
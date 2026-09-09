package core;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class RoundRobinBalancer {
    private final List<Backend> backends;
    private final AtomicInteger index = new AtomicInteger(0);

    public RoundRobinBalancer(List<Backend> backends) {
        if (backends == null || backends.isEmpty()) {
            throw new IllegalArgumentException("Danh sach server khong duoc de trong!");
        }
        this.backends = backends;
    }

    public Backend getNextBackend() {
        // Chi lay nhung backend dang con song (isAlive == true)
        List<Backend> aliveBackends = backends.stream()
                .filter(Backend::isAlive)
                .collect(Collectors.toList());

        if (aliveBackends.isEmpty()) {
            return null; // Tat ca backend deu da chet
        }

        int currentIdx = Math.abs(index.getAndIncrement() % aliveBackends.size());
        return aliveBackends.get(currentIdx);
    }

    public List<Backend> getAllBackends() {
        return backends;
    }
}
package core;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class RoundRobinStrategy implements LoadBalancingStrategy {
    private final AtomicInteger index = new AtomicInteger(0);

    @Override
    public Backend selectBackend(List<Backend> backends, String clientIp) {
        List<Backend> aliveBackends = backends.stream()
                .filter(Backend::isAlive)
                .collect(Collectors.toList());

        if (aliveBackends.isEmpty()) {
            return null;
        }

        int currentIdx = Math.abs(index.getAndIncrement() % aliveBackends.size());
        return aliveBackends.get(currentIdx);
    }
}
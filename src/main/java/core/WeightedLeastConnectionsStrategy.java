package core;

import java.util.List;
import java.util.stream.Collectors;

public class WeightedLeastConnectionsStrategy implements LoadBalancingStrategy {

    @Override
    public Backend selectBackend(List<Backend> backends, String clientIp) {
        List<Backend> aliveBackends = backends.stream()
                .filter(Backend::isAlive)
                .collect(Collectors.toList());

        if (aliveBackends.isEmpty()) {
            return null;
        }

        Backend bestBackend = null;
        double minScore = Double.MAX_VALUE;

        for (Backend backend : aliveBackends) {
            // Score = activeConnections / weight
            double score = (double) backend.getActiveConnections() / backend.getWeight();
            if (score < minScore) {
                minScore = score;
                bestBackend = backend;
            }
        }

        return bestBackend;
    }
}
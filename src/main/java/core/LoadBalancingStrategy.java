package core;

import java.util.List;

public interface LoadBalancingStrategy {
    Backend selectBackend(List<Backend> backends, String clientIp);
}
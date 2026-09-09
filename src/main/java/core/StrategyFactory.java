package core;

public class StrategyFactory {
    public static LoadBalancingStrategy getStrategy(String algorithm) {
        if (algorithm == null) {
            return new WeightedLeastConnectionsStrategy();
        }
        return switch (algorithm.toLowerCase()) {
            case "round_robin" -> new RoundRobinStrategy();
            case "weighted_least_conn", "least_conn" -> new WeightedLeastConnectionsStrategy();
            default -> {
                System.out.println("Thuat toan '" + algorithm + "' khong hop le, mac dinh dung Weighted Least Connections.");
                yield new WeightedLeastConnectionsStrategy();
            }
        };
    }
}
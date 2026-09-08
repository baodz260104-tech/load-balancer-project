import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinBalancer {
    private final List<String> servers;
    private final AtomicInteger index = new AtomicInteger(0);

    public RoundRobinBalancer(List<String> initialServers) {
        if (initialServers == null || initialServers.isEmpty()) {
            throw new IllegalArgumentException("Danh sach server khong duoc de trong!");
        }
        this.servers = new ArrayList<>(initialServers);
    }

    public String getNextServer() {
        if (servers.isEmpty()) {
            return null;
        }
        int currentIdx = Math.abs(index.getAndIncrement() % servers.size());
        return servers.get(currentIdx);
    }

    public List<String> getServers() {
        return Collections.unmodifiableList(servers);
    }
}
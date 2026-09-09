import core.Backend;
import core.RoundRobinBalancer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RoundRobinBalancerTest {

    private RoundRobinBalancer balancer;
    private List<Backend> fixtureServers;

    @BeforeEach
    void setUp() {
        fixtureServers = List.of(
                new Backend("http://localhost:8081"),
                new Backend("http://localhost:8082"),
                new Backend("http://localhost:8083"),
                new Backend("http://localhost:8084")
        );
        balancer = new RoundRobinBalancer(fixtureServers);
    }

    @Test
    @DisplayName("Kiem tra xoay vong server binh thuong khi tat ca deu song")
    void testRoundRobinOrder() {
        assertEquals("http://localhost:8081", balancer.getNextBackend().getUrl());
        assertEquals("http://localhost:8082", balancer.getNextBackend().getUrl());
        assertEquals("http://localhost:8083", balancer.getNextBackend().getUrl());
        assertEquals("http://localhost:8084", balancer.getNextBackend().getUrl());
        assertEquals("http://localhost:8081", balancer.getNextBackend().getUrl());
    }

    @Test
    @DisplayName("Bo qua server chet va chi phan phoi toi server con song")
    void testSkipDeadServer() {
        // Danh dau server 8082 bi chet
        fixtureServers.get(1).setAlive(false);

        assertEquals("http://localhost:8081", balancer.getNextBackend().getUrl());
        assertEquals("http://localhost:8083", balancer.getNextBackend().getUrl()); // Nhay qua 8082
        assertEquals("http://localhost:8084", balancer.getNextBackend().getUrl());
        assertEquals("http://localhost:8081", balancer.getNextBackend().getUrl());
    }
}
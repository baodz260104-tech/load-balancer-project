import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RoundRobinBalancerTest {

    private RoundRobinBalancer balancer;
    private List<String> fixtureServers;

    // Fixture: khoi tao lai tap server mau truoc moi ca test
    @BeforeEach
    void setUp() {
        fixtureServers = List.of(
                "http://localhost:8081",
                "http://localhost:8082",
                "http://localhost:8083",
                "http://localhost:8084"
        );
        balancer = new RoundRobinBalancer(fixtureServers);
    }

    @Test
    @DisplayName("Kiem tra xoay vong server theo dung thu tu 1 -> 2 -> 3 -> 4 -> 1")
    void testRoundRobinOrder() {
        assertEquals("http://localhost:8081", balancer.getNextServer());
        assertEquals("http://localhost:8082", balancer.getNextServer());
        assertEquals("http://localhost:8083", balancer.getNextServer());
        assertEquals("http://localhost:8084", balancer.getNextServer());

        // Quay vong lai server dau tien
        assertEquals("http://localhost:8081", balancer.getNextServer());
    }

    @Test
    @DisplayName("Nem ra ngoai le IllegalArgumentException khi danh sach server rong")
    void testEmptyServerListThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new RoundRobinBalancer(List.of()));
    }
}
import core.Backend;
import core.WeightedLeastConnectionsStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class WeightedLeastConnectionsTest {

    private Backend s1;
    private Backend s2;
    private Backend s3;
    private WeightedLeastConnectionsStrategy strategy;

    @BeforeEach
    void setUp() {
        // s1: weight 1, s2: weight 2, s3: weight 3
        s1 = new Backend("http://localhost:8081", 1);
        s2 = new Backend("http://localhost:8082", 2);
        s3 = new Backend("http://localhost:8083", 3);
        strategy = new WeightedLeastConnectionsStrategy();
    }

    @Test
    void testSelectBackendWithZeroConnections() {
        // Khi tất cả đều có 0 active connection, hệ thống chọn server có sẵn đầu tiên đạt min score
        List<Backend> backends = List.of(s1, s2, s3);
        Backend selected = strategy.selectBackend(backends, "127.0.0.1");
        assertNotNull(selected);
    }

    @Test
    void testPreferBackendWithLowerActiveConnections() {
        List<Backend> backends = List.of(s1, s2);
        // Giả lập s1 đang bận 2 kết nối, s2 đang bận 0 kết nối
        s1.incrementConnections();
        s1.incrementConnections();

        Backend selected = strategy.selectBackend(backends, "127.0.0.1");
        // s2 phải được chọn vì active connections ít hơn hẳn
        assertEquals(s2.getUrl(), selected.getUrl());
    }

    @Test
    void testWeightImpactOnSelection() {
        List<Backend> backends = List.of(s1, s2);
        // s1 (weight 1) có 1 conn -> score = 1.0 / 1 = 1.0
        s1.incrementConnections();

        // s2 (weight 2) có 1 conn -> score = 1.0 / 2 = 0.5
        s2.incrementConnections();

        // s2 có score nhỏ hơn nên phải được chọn tiếp
        Backend selected = strategy.selectBackend(backends, "127.0.0.1");
        assertEquals(s2.getUrl(), selected.getUrl());
    }

    @Test
    void testIgnoreOfflineBackend() {
        List<Backend> backends = List.of(s1, s2);
        s1.setAlive(false); // Đánh dấu s1 sập

        Backend selected = strategy.selectBackend(backends, "127.0.0.1");
        assertEquals(s2.getUrl(), selected.getUrl());
    }
}
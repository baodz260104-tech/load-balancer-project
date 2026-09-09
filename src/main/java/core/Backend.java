package core;

import java.util.concurrent.atomic.AtomicInteger;

public class Backend {
    private final String url;
    private volatile boolean isAlive;
    private final AtomicInteger activeConnections;

    public Backend(String url) {
        this.url = url;
        this.isAlive = true;
        this.activeConnections = new AtomicInteger(0);
    }

    public String getUrl() {
        return url;
    }

    public boolean isAlive() {
        return isAlive;
    }

    public void setAlive(boolean alive) {
        this.isAlive = alive;
    }

    public int getActiveConnections() {
        return activeConnections.get();
    }

    public void incrementConnections() {
        activeConnections.incrementAndGet();
    }

    public void decrementConnections() {
        activeConnections.decrementAndGet();
    }

    @Override
    public String toString() {
        return "Backend{" +
                "url='" + url + '\'' +
                ", isAlive=" + isAlive +
                ", activeConnections=" + activeConnections +
                '}';
    }
}
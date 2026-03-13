package net.studio1122;

import java.util.concurrent.atomic.AtomicInteger;

public class Backend {
    private final String host;
    private final int port;
    private volatile boolean healthy = true;
    private final AtomicInteger activeConnections = new AtomicInteger(0);

    public Backend(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() { return host; }
    public int getPort() { return port; }
    public boolean isHealthy() { return healthy; }
    public void markHealthy(boolean healthy) { this.healthy = healthy; }
    public int getActiveConnections() { return activeConnections.get(); }
    public void incrementConnections() { activeConnections.incrementAndGet(); }
    public void decrementConnections() { activeConnections.decrementAndGet(); }

    @Override
    public String toString() { return host + ":" + port; }
}
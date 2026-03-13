package net.studio1122;

import java.util.List;

public class Config {
    public static final int LISTEN_PORT = 8080;
    public static final int PROXY_BUFFER_SIZE = 4096;

    public static final int THREAD_POOL_SIZE = 50;
    public static final long HEALTH_CHECK_INTERVAL_MS = 5000;
    public static final int HEALTH_CHECK_TIMEOUT_MS = 1000;

    public static final List<Backend> BACKENDS = List.of(
            new Backend("localhost", 9001),
            new Backend("localhost", 9002),
            new Backend("localhost", 9003)
    );
}
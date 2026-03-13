package net.studio1122;

import java.util.List;

public class Config {
    public static final int LISTEN_PORT = 8080;
    public static final int PROXY_BUFFER_SIZE = 4096;

    public static final List<Backend> BACKENDS = List.of(
            new Backend("localhost", 9001),
            new Backend("localhost", 9002),
            new Backend("localhost", 9003)
    );
}
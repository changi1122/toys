package net.studio1122;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * 주기적으로 각 백엔드에 TCP 연결을 시도하여 생존 여부를 확인한다.
 * 상태가 바뀔 때(UP→DOWN, DOWN→UP)만 로그를 출력한다.
 */
public class HealthChecker implements Runnable {
    private final BackendPool pool;
    private final long intervalMs;
    private final int timeoutMs;
    private volatile boolean running = true;
    private Thread checkerThread;

    public HealthChecker(BackendPool pool, long intervalMs, int timeoutMs) {
        this.pool = pool;
        this.intervalMs = intervalMs;
        this.timeoutMs = timeoutMs;
    }

    @Override
    public void run() {
        checkerThread = Thread.currentThread();
        System.out.println("[HealthChecker] Started (interval=" + intervalMs + "ms)");

        while (running) {
            for (Backend backend : pool.getAll()) {
                boolean alive = probe(backend);
                if (alive != backend.isHealthy()) {
                    backend.markHealthy(alive);
                    System.out.println("[HealthChecker] " + backend
                            + " is now " + (alive ? "UP" : "DOWN"));
                }
            }

            try {
                Thread.sleep(intervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        System.out.println("[HealthChecker] Stopped.");
    }

    /**
     * 백엔드에 TCP 연결을 시도한다.
     * connect()에 timeout을 지정하지 않으면 OS 기본값(수십 초)까지 블로킹될 수 있으므로
     * InetSocketAddress + connect(addr, timeout) 형태를 사용한다.
     */
    private boolean probe(Backend backend) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(backend.getHost(), backend.getPort()), timeoutMs);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void stop() {
        running = false;
        if (checkerThread != null) {
            checkerThread.interrupt(); // sleep 중이면 즉시 깨움
        }
    }
}
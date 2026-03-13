package net.studio1122;

import net.studio1122.Algorithm.LeastConnection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TCP 로드밸런서 진입점.
 * ServerSocket으로 클라이언트 연결을 수락하고 ProxyHandler에 위임한다.
 */
public class Main {
    private volatile boolean running = true;
    private ServerSocket serverSocket;
    private HealthChecker healthChecker;
    private ExecutorService executor;

    public static void main(String[] args) {
        new Main().start();
    }

    public void start() {
        BackendPool pool = new BackendPool(Config.BACKENDS, new LeastConnection());
        executor = Executors.newFixedThreadPool(Config.THREAD_POOL_SIZE);

        healthChecker = new HealthChecker(pool,
                Config.HEALTH_CHECK_INTERVAL_MS,
                Config.HEALTH_CHECK_TIMEOUT_MS);
        Thread healthThread = new Thread(healthChecker, "health-checker");
        healthThread.setDaemon(true); // LB가 종료되면 같이 종료
        healthThread.start();

        // JVM 종료 시 (Ctrl+C 포함) 실행되는 훅
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "shutdown-hook"));

        try {
            serverSocket = new ServerSocket(Config.LISTEN_PORT);
            serverSocket.setReuseAddress(true); // TIME_WAIT 상태 포트를 즉시 재사용 가능하게
            System.out.println("[LB] Listening on :" + Config.LISTEN_PORT
                    + " (Least Connection, ThreadPool=" + Config.THREAD_POOL_SIZE + ")");

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept(); // 연결이 올 때까지 블로킹

                    pool.next().ifPresentOrElse(
                            backend -> {
                                System.out.println("[LB] " + clientSocket.getRemoteSocketAddress()
                                        + " → " + backend);
                                executor.submit(new ProxyHandler(clientSocket, backend));
                            },
                            () -> {
                                System.err.println("[LB] No healthy backends, dropping connection");
                                try { clientSocket.close(); } catch (IOException ignored) {}
                            }
                    );

                } catch (SocketException e) {
                    if (!running) break; // stop()에 의해 serverSocket이 닫힌 경우
                    System.err.println("[LB] Accept error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("[LB] Failed to start: " + e.getMessage());
        }

        System.out.println("[LB] Stopped.");
    }

    public void stop() {
        running = false;
        if (healthChecker != null) healthChecker.stop();
        if (executor != null) executor.shutdown(); // 진행 중인 작업은 마무리하고 종료
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {}
    }
}
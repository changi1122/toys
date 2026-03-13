package net.studio1122;

import net.studio1122.Algorithm.LeastConnection;


/**
 * TCP 로드밸런서 진입점.
 * NIO 이벤트 루프(NioEventLoop)와 헬스체커를 시작한다.
 */
public class Main {
    private NioEventLoop eventLoop;
    private HealthChecker healthChecker;

    public static void main(String[] args) {
        new Main().start();
    }

    public void start() {
        BackendPool pool = new BackendPool(Config.BACKENDS, new LeastConnection());

        healthChecker = new HealthChecker(pool,
                Config.HEALTH_CHECK_INTERVAL_MS,
                Config.HEALTH_CHECK_TIMEOUT_MS);
        Thread healthThread = new Thread(healthChecker, "health-checker");
        healthThread.setDaemon(true);
        healthThread.start();

        eventLoop = new NioEventLoop(Config.LISTEN_PORT, pool);

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "shutdown-hook"));

        eventLoop.run(); // 메인 스레드가 이벤트 루프가 됨
    }

    public void stop() {
        if (healthChecker != null) healthChecker.stop();
        if (eventLoop != null) eventLoop.stop();
    }
}
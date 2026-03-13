package net.studio1122;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * TCP 로드밸런서 진입점.
 * ServerSocket으로 클라이언트 연결을 수락하고 ProxyHandler에 위임한다.
 */
public class Main {
    private volatile boolean running = true;
    private ServerSocket serverSocket;

    public static void main(String[] args) {
        new Main().start();
    }

    public void start() {
        Backend backend = new Backend(Config.BACKEND_HOST, Config.BACKEND_PORT);

        // JVM 종료 시 (Ctrl+C 포함) 실행되는 훅
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "shutdown-hook"));

        try {
            serverSocket = new ServerSocket(Config.LISTEN_PORT);
            serverSocket.setReuseAddress(true); // TIME_WAIT 상태 포트를 즉시 재사용 가능하게
            System.out.println("[LB] Listening on :" + Config.LISTEN_PORT
                    + " → backend " + backend);

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept(); // 연결이 올 때까지 블로킹
                    System.out.println("[LB] New connection from "
                            + clientSocket.getRemoteSocketAddress());

                    Thread t = new Thread(new ProxyHandler(clientSocket, backend));
                    t.setDaemon(true); // JVM 종료 시 강제 종료되는 백그라운드 스레드
                    t.start();

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
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {}
    }
}
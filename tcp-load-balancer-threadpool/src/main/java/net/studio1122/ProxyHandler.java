package net.studio1122;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

/**
 * 클라이언트 소켓 ↔ 백엔드 소켓 간 바이트를 양방향으로 중계한다.
 * 연결당 하나의 ProxyHandler 인스턴스가 생성되어 별도 스레드에서 실행된다.
 */
public class ProxyHandler implements Runnable {
    private final Socket clientSocket;
    private final Backend backend;

    public ProxyHandler(Socket clientSocket, Backend backend) {
        this.clientSocket = clientSocket;
        this.backend = backend;
    }

    @Override
    public void run() {
        // try-with-resources: backendSocket은 이 블록이 끝나면 자동으로 close()된다.
        try (Socket backendSocket = new Socket(backend.getHost(), backend.getPort())) {
            backend.incrementConnections();

            InputStream clientIn   = clientSocket.getInputStream();
            OutputStream clientOut = clientSocket.getOutputStream();
            InputStream backendIn  = backendSocket.getInputStream();
            OutputStream backendOut = backendSocket.getOutputStream();

            // 두 relay 스레드가 모두 끝날 때까지 기다리기 위한 래치
            CountDownLatch latch = new CountDownLatch(2);

            // client → backend
            Thread t1 = new Thread(() -> relay(clientIn, backendOut, latch), "relay-c2b");
            // backend → client
            Thread t2 = new Thread(() -> relay(backendIn, clientOut, latch), "relay-b2c");
            t1.start();
            t2.start();

            latch.await(); // 두 relay가 끝날 때까지 이 스레드는 대기

        } catch (IOException e) {
            System.err.println("[ProxyHandler] 연결 실패 (" + backend + "): " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            backend.decrementConnections();
            try { clientSocket.close(); } catch (IOException ignored) {}
        }
    }

    /**
     * in에서 읽어 out으로 쓴다. EOF 또는 오류가 발생하면 out을 닫고 래치를 카운트다운한다.
     * out을 닫으면 상대방의 in.read()가 -1을 반환하여 반대 방향 relay도 자연스럽게 종료된다 (half-close).
     */
    private void relay(InputStream in, OutputStream out, CountDownLatch latch) {
        byte[] buf = new byte[Config.PROXY_BUFFER_SIZE];
        try {
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
                out.flush();
            }
        } catch (IOException e) {
            // 상대방이 연결을 끊으면 정상적으로 발생하는 예외
        } finally {
            latch.countDown();
            try { out.close(); } catch (IOException ignored) {}
        }
    }
}
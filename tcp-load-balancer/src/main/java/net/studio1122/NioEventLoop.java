package net.studio1122;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Optional;

/**
 * NIO Selector 기반 이벤트 루프.
 * 스레드 1개로 모든 클라이언트 연결의 accept/relay를 처리한다.
 *
 * 처리하는 이벤트:
 *   OP_ACCEPT    - 새 클라이언트 연결 수락, 백엔드 채널 논블로킹 연결 시작
 *   OP_CONNECT   - 백엔드 연결 완료, 양쪽 채널을 OP_READ로 전환
 *   OP_READ      - 데이터 수신, 반대편 채널로 즉시 전달
 */
public class NioEventLoop implements Runnable {
    private final int port;
    private final BackendPool pool;
    private volatile boolean running = true;
    private Selector selector;

    public NioEventLoop(int port, BackendPool pool) {
        this.port = port;
        this.pool = pool;
    }

    @Override
    public void run() {
        try {
            selector = Selector.open();

            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);       // 논블로킹 모드로 전환
            serverChannel.bind(new InetSocketAddress(port));
            serverChannel.socket().setReuseAddress(true);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("[NIO] Listening on :" + port);

            while (running) {
                selector.select(); // 이벤트가 하나라도 생길 때까지 블로킹

                Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    iter.remove(); // 처리한 키는 반드시 제거 (안 하면 계속 재처리됨)

                    if (!key.isValid()) continue;

                    if (key.isAcceptable())  handleAccept(key);
                    else if (key.isConnectable()) handleConnect(key);
                    else if (key.isReadable())    handleRead(key);
                }
            }
        } catch (IOException e) {
            if (running) System.err.println("[NIO] Fatal error: " + e.getMessage());
        }

        System.out.println("[NIO] Stopped.");
    }

    /** ① 새 클라이언트 연결 수락 → 백엔드 논블로킹 연결 시작 */
    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = server.accept(); // 논블로킹이므로 null 가능
        if (clientChannel == null) return;

        Optional<Backend> backendOpt = pool.next();
        if (backendOpt.isEmpty()) {
            System.err.println("[NIO] No healthy backends, dropping "
                    + clientChannel.getRemoteAddress());
            clientChannel.close();
            return;
        }

        Backend backend = backendOpt.get();
        System.out.println("[NIO] " + clientChannel.getRemoteAddress() + " → " + backend);

        clientChannel.configureBlocking(false);

        // 백엔드 채널도 논블로킹으로 열고 연결 시작
        // 논블로킹 connect()는 즉시 반환되며, 완료 여부를 OP_CONNECT로 통보받는다
        SocketChannel backendChannel = SocketChannel.open();
        backendChannel.configureBlocking(false);
        backendChannel.connect(new InetSocketAddress(backend.getHost(), backend.getPort()));

        backend.incrementConnections();

        // 연결 완료를 기다리는 동안 두 채널 정보를 ChannelPair로 묶어 attachment에 저장
        ChannelPair pair = new ChannelPair(clientChannel, backendChannel, backend);
        backendChannel.register(selector, SelectionKey.OP_CONNECT, pair);
    }

    /** ② 백엔드 연결 완료 → 양쪽 채널을 OP_READ로 전환 */
    private void handleConnect(SelectionKey key) throws IOException {
        SocketChannel backendChannel = (SocketChannel) key.channel();
        ChannelPair pair = (ChannelPair) key.attachment();

        try {
            // finishConnect()로 연결을 완전히 마무리
            // 연결 실패 시 IOException 발생
            backendChannel.finishConnect();
        } catch (IOException e) {
            System.err.println("[NIO] Backend connect failed: " + e.getMessage());
            closePair(pair);
            key.cancel();
            return;
        }

        // 이제 양쪽에서 데이터를 읽을 준비가 됐으므로 OP_READ로 등록
        // 같은 pair를 attachment로 달아두면 READ 이벤트 때 상대 채널을 찾을 수 있다
        pair.clientChannel.register(selector, SelectionKey.OP_READ, pair);
        key.interestOps(SelectionKey.OP_READ); // backendChannel 키의 관심 이벤트 변경
    }

    /** ③ 데이터 도착 → 반대편 채널로 전달 */
    private void handleRead(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        ChannelPair pair = (ChannelPair) key.attachment();
        SocketChannel peer = pair.getPeer(channel);

        ByteBuffer buf = ByteBuffer.allocate(Config.PROXY_BUFFER_SIZE);
        int n;
        try {
            n = channel.read(buf); // 논블로킹: 현재 읽을 수 있는 만큼만 읽고 즉시 반환
        } catch (IOException e) {
            closePair(pair);
            return;
        }

        if (n == -1) { // EOF: 상대방이 연결을 종료함
            closePair(pair);
            return;
        }

        // buf를 채우기(write) 모드에서 꺼내기(read) 모드로 전환
        // flip() 없이 write()하면 position이 끝에 있어 아무것도 전송되지 않음
        buf.flip();
        try {
            while (buf.hasRemaining()) {
                peer.write(buf);
            }
        } catch (IOException e) {
            closePair(pair);
        }
    }

    private void closePair(ChannelPair pair) {
        if (pair.closed) return; // 한 번만 실행 (양쪽 이벤트가 동시에 EOF일 때 방지)
        pair.closed = true;
        pair.backend.decrementConnections();
        try { pair.clientChannel.close(); } catch (IOException ignored) {}
        try { pair.backendChannel.close(); } catch (IOException ignored) {}
    }

    public void stop() {
        running = false;
        if (selector != null) selector.wakeup(); // select()에서 즉시 깨어나도록
    }

    /**
     * 클라이언트 채널 ↔ 백엔드 채널 쌍을 묶어 SelectionKey의 attachment로 저장한다.
     * READ 이벤트 발생 시 어느 채널에서 왔든 상대 채널을 O(1)로 찾을 수 있다.
     */
    static class ChannelPair {
        final SocketChannel clientChannel;
        final SocketChannel backendChannel;
        final Backend backend;
        boolean closed = false;

        ChannelPair(SocketChannel clientChannel, SocketChannel backendChannel, Backend backend) {
            this.clientChannel = clientChannel;
            this.backendChannel = backendChannel;
            this.backend = backend;
        }

        SocketChannel getPeer(SocketChannel channel) {
            return channel == clientChannel ? backendChannel : clientChannel;
        }
    }
}
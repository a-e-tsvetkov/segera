package segeraroot.quotesource.infra;

import lombok.extern.slf4j.Slf4j;
import segeraroot.connectivity.ConnectionCallback;
import segeraroot.connectivity.Protocol;
import segeraroot.connectivity.util.ByteBufferFactory;
import segeraroot.connectivity.util.MessageDeserializer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.function.Consumer;


@Slf4j
public class Server<T extends ConnectionCallback<ByteBufferFactory> & MessageDeserializer> {
    private final int port;
    private final T connectionCallback;
    private Selector selector;
    private ByteBuffer buffer;

    public Server(int port, T connectionCallback) {
        this.port = port;
        this.connectionCallback = connectionCallback;
    }


    public void start() {
        Thread mainThread = new Thread(this::awaitConnectionsLoop);

        try {
            buffer = ByteBuffer.allocateDirect(1024);
            selector = Selector.open();

            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            log.info("Start server: port={}", port);
            serverSocketChannel.bind(new InetSocketAddress(port));
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mainThread.start();
    }

    private void awaitConnectionsLoop() {
        try {
            while (true) {
                selector.select();
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();
                    if (key.isValid()) {
                        if (key.isAcceptable()) {
                            onAccept(key);
                        } else if (key.isReadable()) {
                            var handler = getHandler(key);
                            handler.read(key);
                        } else if (key.isWritable()) {
                            var handler = getHandler(key);
                            handler.doWrite(key);
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("Exception in await loop", e);
        }
    }

    @SuppressWarnings("unchecked")
    private ConnectionHandler getHandler(SelectionKey key) {
        return (ConnectionHandler) key.attachment();
    }

    private void onClose(ConnectionHandler handler) {
        log.debug("Connection closed: {}", handler.getName());
        connectionCallback.handleCloseConnection(handler);
    }

    private void onAccept(SelectionKey acceptKey) throws IOException {
        final ServerSocketChannel serverSocketChannel = (ServerSocketChannel) acceptKey.channel();
        SocketChannel channel = serverSocketChannel.accept();
        log.debug("Connection accepted: {}", channel.getRemoteAddress());
        channel.configureBlocking(false);
        SelectionKey key = channel.register(selector, SelectionKey.OP_READ);
        var handler = new ConnectionHandler(key);
        key.attach(handler);
        handler.writeHeader();
        connectionCallback.handleNewConnection(handler);
    }

    private class ConnectionHandler extends ConnectionBase<ByteBuffer> implements ByteBufferFactory {

        private final SelectionKey key;
        private final SocketChannel channel;

        private volatile boolean headerIsInBuffer = false;

        public ConnectionHandler(SelectionKey key) {
            this.key = key;
            channel = (SocketChannel) key.channel();

        }

        public void read(SelectionKey key) {
            assert this.key == key;
            try {
                log.trace("Reading from channel: {}", channel.getRemoteAddress());
                buffer.position(0);
                buffer.limit(buffer.capacity());
                int length = channel.read(buffer);
                if (length == -1) {
                    onClose(this);
                    key.cancel();
                    return;
                }
                buffer.flip();
                connectionCallback.onMessage(this, buffer);
            } catch (IOException e) {
                try {
                    channel.close();
                } catch (IOException ioException) {
                    log.error("Error while closing connection", e);
                }
                key.cancel();
                onClose(this);
            }
        }


        public void doWrite(SelectionKey key) throws IOException {
            var socketChannel = (SocketChannel) key.channel();
            log.trace("Writing to channel: {}", socketChannel.getRemoteAddress());
            if (!headerIsInBuffer) {
                log.trace("Writing header: {}", socketChannel.getRemoteAddress());
                buffer.position(0);
                buffer.limit(buffer.capacity());

                buffer.putInt(Protocol.VERSION);
                buffer.flip();
                socketChannel.write(buffer);
                assert buffer.remaining() == 0;
                headerIsInBuffer = true;
            }
            connectionCallback.handleWriting(this, this);
            key.interestOps(SelectionKey.OP_READ);
        }

        @Override
        public void write(Consumer<ByteBuffer> consumer) {
            buffer.position(0);
            buffer.limit(buffer.capacity());
            consumer.accept(buffer);
            buffer.flip();
            try {
                channel.write(buffer);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void startWriting() {
            try {
                log.debug("Start writing {}", channel.getRemoteAddress());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            key.selector().wakeup();
        }

        @Override
        public String getName() {
            return key.channel().toString();
        }

        public void writeHeader() {
            key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            key.selector().wakeup();
        }
    }
}

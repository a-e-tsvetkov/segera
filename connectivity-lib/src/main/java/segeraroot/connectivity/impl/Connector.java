package segeraroot.connectivity.impl;

import lombok.extern.slf4j.Slf4j;
import segeraroot.connectivity.Connection;
import segeraroot.connectivity.ConnectionCallback;
import segeraroot.connectivity.Protocol;
import segeraroot.connectivity.util.ByteBufferFactory;
import segeraroot.connectivity.util.MessageDeserializer;
import segeraroot.connectivity.util.WriteCallback;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public abstract class Connector<T extends ConnectionCallback<ByteBufferFactory> & MessageDeserializer> {
    protected final T connectionCallback;
    protected volatile boolean isRunning = false;
    private Selector selector;
    private ByteBuffer buffer;

    public Connector(T connectionCallback) {
        this.connectionCallback = connectionCallback;
    }

    public final void start() {
        isRunning = true;
        Thread mainThread = new Thread(this::awaitConnectionsLoop);
        try {
            buffer = ByteBuffer.allocateDirect(1024);
            selector = Selector.open();
            onStart(selector);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mainThread.start();
    }

    public void stop() {
        selector.keys().forEach(key -> {
            ConnectionHandler handler = getHandler(key);
            handler.stop();
        });
        isRunning = false;
    }

    protected void onStart(Selector selector) throws IOException {
    }

    private void awaitConnectionsLoop() {
        try {
            while (isRunning) {
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

    protected void onClose(Connection handler) {
        log.debug("Connection closed: {}", handler.getName());
        connectionCallback.handleCloseConnection(handler);
    }

    private void onAccept(SelectionKey acceptKey) throws IOException {
        final ServerSocketChannel serverSocketChannel = (ServerSocketChannel) acceptKey.channel();
        SocketChannel channel = serverSocketChannel.accept();
        connectionEstablished(channel);
    }

    protected final void connectionEstablished(SocketChannel channel) throws IOException {
        log.debug("Connection accepted: {}", channel.getRemoteAddress());
        channel.configureBlocking(false);
        SelectionKey key = channel.register(selector, SelectionKey.OP_READ);
        var handler = new ConnectionHandler(key);
        key.attach(handler);
        handler.writeHeader();
        connectionCallback.handleNewConnection(handler);
    }

    protected void onHeaderRead(ByteBuffer buffer) {

    }

    protected final void readHeader(ByteBuffer buffer) {
        int version = buffer.getInt();
        if (version != Protocol.VERSION) {
            log.error("Wrong version: expected={} actual={}", Protocol.VERSION, version);
            stop();
        }
    }

    protected void onHeaderWrite(ByteBuffer buffer) {
    }

    protected final void doWriteHeader(ByteBuffer buffer) {
        buffer.putInt(Protocol.VERSION);
    }

    private class ConnectionHandler extends ConnectionBase<ByteBuffer> implements ByteBufferFactory {

        private final SelectionKey key;
        private final SocketChannel channel;
        private final SocketAddress remoteAddress;
        private volatile boolean headerRead = false;
        private volatile boolean headerWrite = false;
        private final AtomicInteger writeVersion = new AtomicInteger();

        public ConnectionHandler(SelectionKey key) {
            this.key = key;
            channel = (SocketChannel) key.channel();
            try {
                remoteAddress = channel.getRemoteAddress();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void read(SelectionKey key) {
            assert this.key == key;
            try {
                log.trace("Reading from channel: {}", remoteAddress);
                buffer.position(0);
                buffer.limit(buffer.capacity());
                int length = channel.read(buffer);
                if (length == -1) {
                    onClose(this);
                    key.cancel();
                    return;
                }
                buffer.flip();
                if (!headerRead) {
                    onHeaderRead(Connector.this.buffer);
                    headerRead = true;
                }
                connectionCallback.onMessage(this, buffer);
            } catch (IOException e) {
                log.error("Error in connection connection", e);
                channelClose();
            }
        }


        private void channelClose() {
            try {
                channel.close();
            } catch (IOException e) {
                log.error("Error while closing connection", e);
            }
            key.cancel();
            onClose(this);
        }


        public void doWrite(SelectionKey key) throws IOException {
            var socketChannel = (SocketChannel) key.channel();
            log.trace("Writing to channel: {}", remoteAddress);
            int writeVersionStart = writeVersion.get();
            if (!headerWrite) {
                log.trace("Writing header: {}", remoteAddress);
                buffer.position(0);
                buffer.limit(buffer.capacity());

                onHeaderWrite(Connector.this.buffer);
                buffer.flip();
                socketChannel.write(buffer);
                assert buffer.remaining() == 0;
                headerWrite = true;
            }
            ConnectionCallback.WritingResult success = connectionCallback.handleWriting(this, this);
            if (success == ConnectionCallback.WritingResult.DONE) {
                if (writeVersionStart == writeVersion.get())
                    key.interestOps(SelectionKey.OP_READ);
            }
        }

        @Override
        public boolean write(WriteCallback consumer) {
            buffer.position(0);
            buffer.limit(buffer.capacity());
            boolean success = consumer.tryWrite(buffer);
            if (!success) {
                return false;
            }
            buffer.flip();
            try {
                channel.write(buffer);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return true;
        }

        @Override
        public void startWriting() {
            log.trace("Start writing {}", remoteAddress);
            writeVersion.incrementAndGet();
            key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            key.selector().wakeup();
        }

        @Override
        public String getName() {
            return key.channel().toString();
        }

        public void writeHeader() {
            startWriting();
        }

        public void stop() {
            channelClose();
        }
    }
}

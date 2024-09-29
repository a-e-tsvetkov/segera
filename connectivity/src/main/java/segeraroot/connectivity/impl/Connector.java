package segeraroot.connectivity.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import segeraroot.connectivity.Connection;
import segeraroot.connectivity.callbacks.ConnectivityChanel;
import segeraroot.connectivity.callbacks.ConnectivityHandler;
import segeraroot.connectivity.callbacks.WritingResult;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RequiredArgsConstructor
public abstract class Connector {
    private final ConnectivityHandler handler;

    protected volatile boolean isRunning = false;
    private Selector selector;

    public final void start() {
        isRunning = true;
        try {
            selector = Selector.open();
            onStart(selector);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Thread mainThread = new Thread(this::mainLoop);
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

    private void mainLoop() {
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

    private ConnectionHandler getHandler(SelectionKey key) {
        return (ConnectionHandler) key.attachment();
    }

    protected void onClose(Connection connection) {
        log.debug("Connection closed: {}", connection.getName());
        handler.handleCloseConnection(connection);
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
        var connection = new ConnectionHandler(key);
        key.attach(connection);
        Object context = handler.handleNewConnection(connection);
        connection.setContext(context);
    }

    private class ConnectionHandler extends ConnectionBase implements ConnectivityChanel {

        private final SelectionKey key;
        private final SocketChannel channel;
        private final AtomicInteger writeVersion = new AtomicInteger();

        public ConnectionHandler(SelectionKey key) {
            this.key = key;
            channel = (SocketChannel) key.channel();
        }

        public void read(SelectionKey key) {
            assert this.key == key;
            try {
                log.trace("read: Reading from channel: {}", channel);
                handler.read(this, this);
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
            log.trace("doWrite: Writing to channel: {}", getName());
            int writeVersionStart = writeVersion.get();
            var success = handler.write(this, this);
            if (success == WritingResult.DONE) {
                if (writeVersionStart == writeVersion.get()) {
                    //TODO: Add proper synchronization. Currently write request can be lost if happens here.
                    key.interestOps(SelectionKey.OP_READ);
                }
            }
        }

        @Override
        public void startWriting() {
            log.trace("startWriting: request writing to {}", getName());
            writeVersion.incrementAndGet();
            key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            key.selector().wakeup();
        }

        @Override
        public String getName() {
            return key.channel().toString();
        }

        public void stop() {
            channelClose();
        }

        @Override
        public void read(ByteBuffer buffer) throws IOException {
            int length = channel.read(buffer);
            log.trace("read: read = {}", length);
            if (length == -1) {
                onClose(this);
                key.cancel();
            }
        }

        @Override
        public void write(ByteBuffer buffer) throws IOException {
            int written = channel.write(buffer);
            log.trace("write: written = {} remaining = {}", written, buffer.remaining());
            assert buffer.remaining() == 0;
        }
    }
}

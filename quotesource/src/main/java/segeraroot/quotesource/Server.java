package segeraroot.quotesource;

import lombok.extern.slf4j.Slf4j;
import segeraroot.quotemodel.*;
import segeraroot.quotemodel.messages.Quote;
import segeraroot.quotemodel.messages.Subscribe;
import segeraroot.quotemodel.messages.Unsubscribe;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;


@Slf4j
public class Server {
    private final int port;
    private final QuoteConnectionCallback connectionCallback;
    private Selector selector;
    private ByteBuffer buffer;

    public Server(int port, QuoteConnectionCallback connectionCallback) {
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
                            var handler = (ConnectionHandler) key.attachment();
                            handler.read(key);
                        } else if (key.isWritable()) {
                            var handler = (ConnectionHandler) key.attachment();
                            handler.doWrite(key);
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("Exception in await loop", e);
        }
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

    private class ConnectionHandler implements QuoteConnection {

        private final SelectionKey key;
        private ReadingState readingState = ReadingState.START;
        private MessageType messageType;
        private ByteBuffer messageBodyBuffer;
        private final Queue<MessageWrapper<Message>> writeQueue = new ConcurrentLinkedQueue<>();
        private Object context;
        private volatile boolean headerIsInBuffer = false;

        public ConnectionHandler(SelectionKey key) {
            this.key = key;
        }

        public void read(SelectionKey key) {
            assert this.key == key;
            var socketChannel = (SocketChannel) key.channel();
            try {
                log.trace("Reading from channel: {}", socketChannel.getRemoteAddress());
                buffer.position(0);
                buffer.limit(buffer.capacity());
                int length = socketChannel.read(buffer);
                if (length == -1) {
                    onClose(this);
                    key.cancel();
                    return;
                }
                buffer.flip();
            } catch (IOException e) {
                try {
                    socketChannel.close();
                } catch (IOException ioException) {
                    log.error("Error while closing connection", e);
                }
                key.cancel();
                onClose(this);
                return;
            }

            processMesage();
        }

        private void processMesage() {
            while (buffer.hasRemaining()) {
                switch (readingState) {
                    case START:
                        messageType = MessageType.valueOf(buffer.get());
                        readingState = ReadingState.IN_MESSAGE;
                        messageBodyBuffer = ByteBuffer.allocate(messageType.getSize());
                        break;
                    case IN_MESSAGE:
                        ByteBufferUtil.copy(buffer, messageBodyBuffer);
                        if (messageBodyBuffer.remaining() == 0) {
                            messageBodyBuffer.flip();
                            processMessage(messageType, messageBodyBuffer);
                            readingState = ReadingState.START;
                            messageType = null;
                            messageBodyBuffer = null;
                        }
                        break;
                }
            }
        }

        private void processMessage(MessageType messageType, ByteBuffer messageBodyBuffer) {
            Message value;
            switch (messageType) {
                case SUBSCRIBE: {
                    byte[] bytes = new byte[Quote.SYMBOL_LENGTH];
                    messageBodyBuffer.get(bytes);
                    value = Subscribe.builder()
                            .symbol(new String(bytes, StandardCharsets.US_ASCII))
                            .build();
                    break;
                }
                case UNSUBSCRIBE: {
                    byte[] bytes = new byte[Quote.SYMBOL_LENGTH];
                    messageBodyBuffer.get(bytes);
                    value = Unsubscribe.builder()
                            .symbol(new String(bytes, StandardCharsets.US_ASCII))
                            .build();
                    break;
                }
                case QUOTE:
                case SYMBOLS_REQ:
                case SYMBOLS_RES:
                default:
                    throw new RuntimeException("Unexpected message type: " + messageType);
            }
            connectionCallback.handleMessageConnection(this, MessageWrapper.builder()
                    .type(messageType)
                    .value(value)
                    .build());
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
            MessageWrapper<Message> messageWrapper;
            while ((messageWrapper = writeQueue.poll()) != null) {
                log.trace("Writing message: {} {}", socketChannel.getRemoteAddress(), messageWrapper.getType());

                buffer.position(0);
                buffer.limit(buffer.capacity());
                buffer.put(messageWrapper.getType().getCode());
                switch (messageWrapper.getType()) {
                    case QUOTE:
                        doWriteQuote(buffer, (Quote) messageWrapper.getValue());
                        break;
                    case SUBSCRIBE:
                    case UNSUBSCRIBE:
                    case SYMBOLS_REQ:
                    case SYMBOLS_RES:
                    default:
                        throw new RuntimeException("Unexpected type: " + messageWrapper.getType());
                }
                buffer.flip();
                socketChannel.write(buffer);
                assert buffer.remaining() == 0;
            }
            key.interestOps(SelectionKey.OP_READ);
        }

        private void doWriteQuote(ByteBuffer buffer, Quote quote) {
            writeSymbol(buffer, quote.getSymbol());
            buffer.putLong(Instant.now().toEpochMilli());
            buffer.putLong(quote.getVolume());
            buffer.putLong(quote.getPrice());
        }

        private void writeSymbol(ByteBuffer buffer, String symbol) {
            assert symbol.length() == Quote.SYMBOL_LENGTH;
            buffer.put(symbol.getBytes(StandardCharsets.US_ASCII));
        }

        @Override
        public void write(MessageWrapper<Message> messageWrapper) {
            log.debug("Add mesage to queue: {}", messageWrapper.getType());
            writeQueue.add(messageWrapper);
            key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            key.selector().wakeup();

        }

        @Override
        public Object get() {
            return context;
        }

        @Override
        public void set(Object context) {
            this.context = context;
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

    private enum ReadingState {
        START,
        IN_MESSAGE
    }
}

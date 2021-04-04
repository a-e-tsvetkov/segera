package segeraroot.quotesource;

import segeraroot.quotemodel.*;
import segeraroot.quotemodel.messages.Quote;
import segeraroot.quotemodel.messages.Subscribe;
import segeraroot.quotemodel.messages.Unsubscribe;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class Serializer {
    public QuoteConnectionCallback<ByteBuffer> wrap(QuoteConnectionCallback<MessageWrapper> callback) {
        return SimpleQuoteConnectionCallback.<ByteBuffer>builder()
                .newHandler(connection ->
                        callback.handleNewConnection(wrap(connection)))
                .closeHandler(connection ->
                        callback.handleCloseConnection(wrap(connection)))
                .messageHandler((connection, byteBuffer) ->
                        wrap(callback, connection, byteBuffer))
                .build();
    }

    private void wrap(QuoteConnectionCallback<MessageWrapper> callback, QuoteConnection<ByteBuffer> connection, ByteBuffer buffer) {
        var wrapper = (SerializingConnection) connection.get();
        wrapper.onMessage(callback, connection, buffer);
    }

    private QuoteConnection<MessageWrapper> wrap(QuoteConnection<ByteBuffer> connection) {
        var wrapper = (SerializingConnection) connection.get();
        if (wrapper == null) {
            wrapper = new SerializingConnection(connection);
            connection.set(wrapper);
        }
        return wrapper;

    }


    private static class SerializingConnection extends QuoteConnectionBase<MessageWrapper> {
        private final QuoteConnection<ByteBuffer> connection;
        private ReadingState readingState = ReadingState.START;
        private MessageType messageType;
        private ByteBuffer messageBodyBuffer;

        public SerializingConnection(QuoteConnection<ByteBuffer> connection) {
            this.connection = connection;
        }

        @Override
        public void write(MessageWrapper messageWrapper) {
            connection.write(serialize(messageWrapper));
        }

        private ByteBuffer serialize(MessageWrapper messageWrapper) {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
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
            return buffer;
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
        public String getName() {
            return "W|" + connection.getName();
        }

        public void onMessage(QuoteConnectionCallback<MessageWrapper> callback, QuoteConnection<ByteBuffer> connection, ByteBuffer buffer) {
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
                            processMessage(callback, messageType, messageBodyBuffer);
                            readingState = ReadingState.START;
                            messageType = null;
                            messageBodyBuffer = null;
                        }
                        break;
                }
            }
        }


        private void processMessage(QuoteConnectionCallback<MessageWrapper> callback, MessageType messageType, ByteBuffer messageBodyBuffer) {

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
            callback.handleMessageConnection(this, MessageWrapper.builder()
                    .type(messageType)
                    .value(value)
                    .build());
        }

        private enum ReadingState {
            START,
            IN_MESSAGE
        }
    }
}

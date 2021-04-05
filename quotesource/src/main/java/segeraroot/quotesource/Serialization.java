package segeraroot.quotesource;

import segeraroot.quotemodel.Message;
import segeraroot.quotemodel.MessageType;
import segeraroot.quotemodel.MessageWrapper;
import segeraroot.quotemodel.messages.Quote;
import segeraroot.quotemodel.messages.Subscribe;
import segeraroot.quotemodel.messages.Unsubscribe;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class Serialization {
    public ByteBuffer serialize(MessageWrapper messageWrapper) {
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

    //*******************************************************

    private ReadingState readingState = ReadingState.START;
    private MessageType messageType;
    private ByteBuffer messageBodyBuffer;

    public void onMessage(ByteBuffer buffer, MessageSink<MessageWrapper> messageSink) {
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
                        processMessage(messageType, messageBodyBuffer, messageSink);
                        readingState = ReadingState.START;
                        messageType = null;
                        messageBodyBuffer = null;
                    }
                    break;
            }
        }
    }


    private void processMessage(MessageType messageType, ByteBuffer messageBodyBuffer, MessageSink<MessageWrapper> messageSink) {

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
        messageSink.send(MessageWrapper.builder()
                .type(messageType)
                .value(value)
                .build());
    }

    private enum ReadingState {
        START,
        IN_MESSAGE
    }
}

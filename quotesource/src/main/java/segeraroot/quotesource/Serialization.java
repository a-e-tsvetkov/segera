package segeraroot.quotesource;

import segeraroot.quotemodel.Message;
import segeraroot.quotemodel.MessageType;
import segeraroot.quotemodel.QuoteConstant;
import segeraroot.quotemodel.messages.Quote;
import segeraroot.quotemodel.messages.Subscribe;
import segeraroot.quotemodel.messages.Unsubscribe;

import java.nio.ByteBuffer;

public class Serialization {
    public ByteBuffer serialize(Message message) {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.put((byte) message.messageType().ordinal());
        switch (message.messageType()) {
            case Quote:
                doWriteQuote(buffer, (Quote) message);
                break;
            case Subscribe:
            case Unsubscribe:
            default:
                throw new RuntimeException("Unexpected type: " + message.messageType());
        }
        buffer.flip();
        return buffer;
    }

    private void doWriteQuote(ByteBuffer buffer, Quote quote) {
        writeSymbol(buffer, quote.symbol());
        buffer.putLong(quote.volume());
        buffer.putLong(quote.price());
        buffer.putLong(quote.date());
    }

    private void writeSymbol(ByteBuffer buffer, byte[] symbol) {
        assert symbol.length == QuoteConstant.SYMBOL_LENGTH;
        buffer.put(symbol);
    }

    //*******************************************************

    private ReadingState readingState = ReadingState.START;
    private MessageType messageType;
    private ByteBuffer messageBodyBuffer;

    public void onMessage(ByteBuffer buffer, MessageSink<Message> messageSink) {
        while (buffer.hasRemaining()) {
            switch (readingState) {
                case START:
                    messageType = toType(buffer.get());
                    readingState = ReadingState.IN_MESSAGE;
                    messageBodyBuffer = ByteBuffer.allocate(size(messageType));
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

    private int size(MessageType messageType) {
        switch (messageType) {
            case Subscribe:
                return 3;
            case Unsubscribe:
                return 3;
            case Quote:
                return 11;
            default:
                throw new RuntimeException("Unexpected message type " + messageType);
        }
    }

    private MessageType toType(byte b) {
        switch (b) {
            case 0:
                return MessageType.Quote;
            case 1:
                return MessageType.Subscribe;
            case 2:
                return MessageType.Unsubscribe;
            default:
                throw new RuntimeException("Unexpected message type " + b);
        }
    }

    private void processMessage(
            MessageType messageType,
            ByteBuffer messageBodyBuffer,
            MessageSink<Message> messageSink) {

        Message value;
        switch (messageType) {
            case Subscribe: {
                byte[] bytes = new byte[QuoteConstant.SYMBOL_LENGTH];
                messageBodyBuffer.get(bytes);
                value = Subscribe.builder()
                        .symbol(bytes)
                        .build();
                break;
            }
            case Unsubscribe: {
                byte[] bytes = new byte[QuoteConstant.SYMBOL_LENGTH];
                messageBodyBuffer.get(bytes);
                value = Unsubscribe.builder()
                        .symbol(bytes)
                        .build();
                break;
            }
            case Quote:
            default:
                throw new RuntimeException("Unexpected message type: " + messageType);
        }
        messageSink.send(value);
    }

    private enum ReadingState {
        START,
        IN_MESSAGE
    }
}

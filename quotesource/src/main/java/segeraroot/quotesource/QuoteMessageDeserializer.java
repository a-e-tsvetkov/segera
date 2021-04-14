package segeraroot.quotesource;

import segeraroot.connectivity.Connection;
import segeraroot.quotemodel.MessageType;
import segeraroot.quotemodel.QuoteConstant;
import segeraroot.quotemodel.ReadersVisitor;
import segeraroot.quotemodel.messages.Quote;
import segeraroot.quotemodel.messages.Subscribe;
import segeraroot.quotemodel.messages.Unsubscribe;
import segeraroot.quotemodel.writers.QuoteReader;
import segeraroot.quotemodel.writers.SubscribeReader;
import segeraroot.quotemodel.writers.UnsubscribeReader;
import segeraroot.quotesource.infra.MessageDeserializer;
import segeraroot.quotesource.infra.MessageDeserializerComponent;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class QuoteMessageDeserializer implements MessageDeserializer {


    private final ReadersVisitor<?> callback;

    public QuoteMessageDeserializer(ReadersVisitor<?> callback) {
        this.callback = callback;
    }

    private enum ReadingState {
        START,
        IN_MESSAGE
    }

    private ReadingState readingState = ReadingState.START;
    private MessageType messageType;

    private final QuoteDeserializer quoteDeserializer = new QuoteDeserializer();
    private final SubscribeDeserializer subscribeDeserializer = new SubscribeDeserializer();
    private final UnsubscribeDeserializer unsubscribeDeserializer = new UnsubscribeDeserializer();

    @Override
    public void onMessage(Connection connection, ByteBuffer buffer) {
        switch (readingState) {
            case START:
                MessageType messageType = toType(buffer.get());
                readingState = ReadingState.IN_MESSAGE;
                this.messageType = messageType;
                break;
            case IN_MESSAGE:
                switch (this.messageType) {
                    case Quote:
                        if (quoteDeserializer.onMessage(buffer)) {
                            readingState = ReadingState.START;
                            callback.visit(connection, quoteDeserializer);
                            quoteDeserializer.reset();
                        }
                        break;
                    case Subscribe:
                        if (subscribeDeserializer.onMessage(buffer)) {
                            readingState = ReadingState.START;
                            callback.visit(connection, subscribeDeserializer);
                            subscribeDeserializer.reset();
                        }
                        break;
                    case Unsubscribe:
                        if (unsubscribeDeserializer.onMessage(buffer)) {
                            readingState = ReadingState.START;
                            callback.visit(connection, unsubscribeDeserializer);
                            unsubscribeDeserializer.reset();
                        }
                        break;
                }
                break;
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

    private static class QuoteDeserializer implements MessageDeserializerComponent<Quote>, QuoteReader {
        private final ByteArrayDeserializer symbol = new ByteArrayDeserializer(QuoteConstant.SYMBOL_LENGTH);
        private final LongDeserializer volume = new LongDeserializer();
        private final LongDeserializer price = new LongDeserializer();
        private final LongDeserializer date = new LongDeserializer();

        private int position;

        // |0 |1 |2 |3 |4 |5 |6 |7 |8 |9 |10|11|12|13|14|
        // |symbol  |volume     |price      |date       |
        @Override
        public boolean onMessage(ByteBuffer buffer) {
            switch (position) {
                case 0:
                    if (symbol.onMessage(buffer)) {
                        position = 1;
                    }
                case 1:
                    if (symbol.onMessage(buffer)) {
                        position = 2;
                    }
                case 2:
                    if (symbol.onMessage(buffer)) {
                        position = 3;
                    }
                case 3:
                    if (symbol.onMessage(buffer)) {
                        return true;
                    }
            }
            return false;
        }

        @Override
        public Quote getValue() {
            return Quote.builder()
                    .symbol(symbol.getValue())
                    .price(price.getValue())
                    .volume(volume.getValue())
                    .date(date.getValue())
                    .build();
        }

        @Override
        public byte[] symbol() {
            return symbol.getValue();
        }

        @Override
        public long volume() {
            return volume.getValue();
        }

        @Override
        public long price() {
            return price.getValue();
        }

        @Override
        public long date() {
            return date.getValue();
        }

        @Override
        public void reset() {
            position = 0;
            symbol.reset();
            price.reset();
            volume.reset();
            date.reset();
        }
    }

    private static class SubscribeDeserializer implements MessageDeserializerComponent<Subscribe>, SubscribeReader {
        private final ByteArrayDeserializer symbol = new ByteArrayDeserializer(QuoteConstant.SYMBOL_LENGTH);

        @Override
        public boolean onMessage(ByteBuffer buffer) {
            return symbol.onMessage(buffer);
        }

        @Override
        public Subscribe getValue() {
            return Subscribe.builder()
                    .symbol(Arrays.copyOf(symbol.getValue(), symbol.length))
                    .build();
        }

        @Override
        public byte[] symbol() {
            return symbol.getValue();
        }

        @Override
        public void reset() {
            symbol.reset();
        }
    }

    private static class UnsubscribeDeserializer implements MessageDeserializerComponent<Unsubscribe>, UnsubscribeReader {
        private final ByteArrayDeserializer symbol = new ByteArrayDeserializer(QuoteConstant.SYMBOL_LENGTH);


        @Override
        public boolean onMessage(ByteBuffer buffer) {
            return symbol.onMessage(buffer);
        }

        @Override
        public Unsubscribe getValue() {
            return Unsubscribe.builder()
                    .symbol(Arrays.copyOf(symbol.getValue(), symbol.length))
                    .build();
        }

        @Override
        public byte[] symbol() {
            return symbol.getValue();
        }

        @Override
        public void reset() {
            symbol.reset();
        }
    }

    static class LongDeserializer {
        private int position;
        private long value;

        public boolean onMessage(ByteBuffer buffer) {
            while (buffer.hasRemaining()) {
                byte b = buffer.get();
                value = value << 8;
                value |= b & 0xffL;
                position++;
                if (position == 8) {
                    return true;
                }
            }
            return false;
        }

        public long getValue() {
            return value;
        }

        public void reset() {
            position = 0;
        }
    }


    private static class ByteArrayDeserializer {
        private final int length;
        private final byte[] bytes;
        private int position = 0;

        public ByteArrayDeserializer(int symbolLength) {
            length = symbolLength;
            bytes = new byte[length];
        }

        public boolean onMessage(ByteBuffer buffer) {
            int length = Math.min(bytes.length - position, buffer.remaining());
            buffer.get(bytes, position, length);
            position += length;
            return position == bytes.length;
        }

        public byte[] getValue() {
            return bytes;
        }

        public void reset() {
            position = 0;
        }
    }
}

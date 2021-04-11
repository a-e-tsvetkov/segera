package segeraroot.quotesource;

import segeraroot.quotemodel.Message;
import segeraroot.quotemodel.MessageType;
import segeraroot.quotemodel.QuoteConstant;
import segeraroot.quotemodel.messages.Quote;
import segeraroot.quotemodel.messages.Subscribe;
import segeraroot.quotemodel.messages.Unsubscribe;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class QuoteMessageDeserializer implements MessageDeserializer<Message> {


    private enum ReadingState {
        START,
        IN_MESSAGE
    }

    private ReadingState readingState = ReadingState.START;
    private MessageDeserializer<? extends Message> messageDeserializer;
    private final MessageDeserializer<? extends Message>[] messageDeserializers;

    {
        //noinspection unchecked
        messageDeserializers = new MessageDeserializer[]{
                new QuoteDeserializer(),
                new SubscribeDeserializer(),
                new UnsubscribeDeserializer(),
        };
    }


    @Override
    public boolean onMessage(ByteBuffer buffer) {
        switch (readingState) {
            case START:
                MessageType messageType = toType(buffer.get());
                readingState = ReadingState.IN_MESSAGE;
                messageDeserializer = messageDeserializers[messageType.ordinal()];
                messageDeserializer.reset();
                break;
            case IN_MESSAGE:
                if (messageDeserializer.onMessage(buffer)) {
                    readingState = ReadingState.START;
                    return true;
                }
                break;
        }

        return false;
    }

    @Override
    public Message getValue() {
        return messageDeserializer.getValue();
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

    private static class QuoteDeserializer implements MessageDeserializer<Quote> {
        private final MessageDeserializer<byte[]> symbol = new ByteArrayDeserializer(QuoteConstant.SYMBOL_LENGTH);
        private final MessageDeserializer<Long> volume = new LongDeserializer();
        private final MessageDeserializer<Long> price = new LongDeserializer();
        private final MessageDeserializer<Long> date = new LongDeserializer();

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
        public void reset() {
            position = 0;
            symbol.reset();
            price.reset();
            volume.reset();
            date.reset();
        }
    }

    private static class SubscribeDeserializer implements MessageDeserializer<Subscribe> {

        byte[] symbol = new byte[QuoteConstant.SYMBOL_LENGTH];
        private int position;

        @Override
        public boolean onMessage(ByteBuffer buffer) {
            int length = Math.min(symbol.length - position, buffer.remaining());
            buffer.get(symbol, position, length);
            position += length;
            return position == symbol.length;
        }

        @Override
        public Subscribe getValue() {
            return Subscribe.builder()
                    .symbol(Arrays.copyOf(symbol, symbol.length))
                    .build();
        }

        @Override
        public void reset() {
            position = 0;
        }
    }

    private static class UnsubscribeDeserializer implements MessageDeserializer<Unsubscribe> {
        byte[] symbol = new byte[QuoteConstant.SYMBOL_LENGTH];
        private int position;

        @Override
        public boolean onMessage(ByteBuffer buffer) {
            int length = Math.min(symbol.length - position, buffer.remaining());
            buffer.get(symbol, position, length);
            position += length;
            return position == symbol.length;
        }

        @Override
        public Unsubscribe getValue() {
            return Unsubscribe.builder()
                    .symbol(Arrays.copyOf(symbol, symbol.length))
                    .build();
        }

        @Override
        public void reset() {
            position = 0;
        }
    }

    static class LongDeserializer implements MessageDeserializer<Long> {
        private int position;
        private long value;

        @Override
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

        @Override
        public Long getValue() {
            return value;
        }

        @Override
        public void reset() {
            position = 0;
        }
    }


    private static class ByteArrayDeserializer implements MessageDeserializer<byte[]> {
        private int position;

        public ByteArrayDeserializer(int symbolLength) {
        }

        @Override
        public boolean onMessage(ByteBuffer buffer) {
            return false;
        }

        @Override
        public byte[] getValue() {
            return new byte[0];
        }

        @Override
        public void reset() {
            position = 0;
        }
    }
}

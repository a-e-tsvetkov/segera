package segeraroot.quotesource;

import segeraroot.connectivity.Connection;
import segeraroot.connectivity.util.ByteArrayDeserializer;
import segeraroot.connectivity.util.LongDeserializer;
import segeraroot.connectivity.util.MessageDeserializerBase;
import segeraroot.quotemodel.MessageType;
import segeraroot.quotemodel.QuoteConstant;
import segeraroot.quotemodel.ReadersVisitor;
import segeraroot.quotemodel.messages.Quote;
import segeraroot.quotemodel.messages.Subscribe;
import segeraroot.quotemodel.messages.Unsubscribe;
import segeraroot.quotemodel.writers.QuoteReader;
import segeraroot.quotemodel.writers.SubscribeReader;
import segeraroot.quotemodel.writers.UnsubscribeReader;
import segeraroot.quotesource.infra.MessageDeserializerComponent;

import java.nio.ByteBuffer;

public class QuoteMessageDeserializer
        extends MessageDeserializerBase<ReadersVisitor<?>, MessageType> {


    public QuoteMessageDeserializer(ReadersVisitor<?> callback) {
        super(callback);
    }

    private final QuoteDeserializer quoteDeserializer = new QuoteDeserializer();
    private final SubscribeDeserializer subscribeDeserializer = new SubscribeDeserializer();
    private final UnsubscribeDeserializer unsubscribeDeserializer = new UnsubscribeDeserializer();

    protected void parseBody(Connection connection, ByteBuffer buffer) {
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
    }

    protected MessageType toType(byte b) {
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
        public byte[] symbol() {
            return symbol.getValue();
        }

        @Override
        public void reset() {
            symbol.reset();
        }
    }


}

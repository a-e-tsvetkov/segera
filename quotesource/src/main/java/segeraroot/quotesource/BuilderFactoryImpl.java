package segeraroot.quotesource;

import segeraroot.connectivity.ByteBufferFactory;
import segeraroot.connectivity.ByteBufferHolder;
import segeraroot.quotemodel.BuilderFactory;
import segeraroot.quotemodel.QuoteConstant;
import segeraroot.quotemodel.readers.QuoteBuilder;
import segeraroot.quotemodel.readers.SubscribeBuilder;
import segeraroot.quotemodel.readers.UnsubscribeBuilder;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public class BuilderFactoryImpl implements BuilderFactory, ByteBufferHolder {
    private final QuoteBuilder quoteBuilder = new QuoteBuilderImpl();
    private volatile ByteBufferFactory byteBufferFactory;

    @Override
    public void set(ByteBufferFactory byteBufferFactory) {
        this.byteBufferFactory = byteBufferFactory;
    }

    @Override
    public QuoteBuilder createQuoteBuilder() {
        return quoteBuilder;
    }

    @Override
    public SubscribeBuilder createSubscribeBuilder() {
        throw new RuntimeException();
    }

    @Override
    public UnsubscribeBuilder createUnsubscribeBuilder() {
        throw new RuntimeException();
    }

    private class QuoteBuilderImpl implements QuoteBuilder, Consumer<ByteBuffer> {
        private final byte[] symbol = new byte[QuoteConstant.SYMBOL_LENGTH];
        private long volume;
        private long price;
        private long date;

        @Override
        public QuoteBuilder symbol(byte[] value) {
            System.arraycopy(value, 0, symbol, 0, QuoteConstant.SYMBOL_LENGTH);
            return this;
        }

        @Override
        public QuoteBuilder volume(long value) {
            volume = value;
            return this;
        }

        @Override
        public QuoteBuilder price(long value) {
            price = value;
            return this;
        }

        @Override
        public QuoteBuilder date(long value) {
            date = value;
            return this;
        }

        @Override
        public void send() {
            byteBufferFactory.write(this);

        }

        @Override
        public void accept(ByteBuffer buffer) {
            buffer.put((byte) 0);
            buffer.put(symbol);
            buffer.putLong(volume);
            buffer.putLong(price);
            buffer.putLong(date);
        }
    }
}

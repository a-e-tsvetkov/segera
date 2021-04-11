package segeraroot.quotesource;

import segeraroot.quotemodel.Message;
import segeraroot.quotemodel.QuoteConstant;
import segeraroot.quotemodel.messages.Quote;

import java.nio.ByteBuffer;

public class QuoteMessageSerialization implements Serialization<Message> {
    @Override
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
}

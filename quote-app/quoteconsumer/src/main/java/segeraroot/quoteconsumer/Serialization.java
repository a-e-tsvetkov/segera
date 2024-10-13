package segeraroot.quoteconsumer;


import segeraroot.quoteconsumer.messages.Quote;
import segeraroot.quoteconsumer.messages.Subscribe;
import segeraroot.quoteconsumer.messages.Unsubscribe;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class Serialization {

    public void writeHeader(DataOutputStream stream) throws IOException {
        stream.writeInt(Protocol.VERSION);
    }

    public int readHeader(DataInputStream stream) throws IOException, QuoteProtocolWrongVersionException {
        int version = stream.readInt();
        if (version != Protocol.VERSION) {
            throw new QuoteProtocolWrongVersionException(Protocol.VERSION, version);
        }
        return version;
    }

    public void writeMessage(MessageWrapper messageWrapper, DataOutputStream stream) throws IOException {
        stream.writeByte(messageWrapper.getType().getCode());
        switch (messageWrapper.getType()) {
            case SUBSCRIBE:
                writeSubscribe((Subscribe) messageWrapper.getValue(), stream);
                break;
            case UNSUBSCRIBE:
                writeUnsubscribe((Unsubscribe) messageWrapper.getValue(), stream);
                break;
            case QUOTE:
                writeQuote((Quote) messageWrapper.getValue(), stream);
                break;
        }
    }

    public MessageWrapper readMessage(DataInputStream stream) throws IOException {
        byte typeByte = stream.readByte();
        MessageType messageType = MessageType.valueOf(typeByte);
        Message value = switch (messageType) {
            case SUBSCRIBE -> readSubscribe(stream);
            case UNSUBSCRIBE -> readUnsubscribe(stream);
            case QUOTE -> readQuote(stream);
        };
        return MessageWrapper.builder()
            .type(messageType)
            .value(value)
            .build();
    }

    private void writeSubscribe(Subscribe subscribe, DataOutputStream stream) throws IOException {
        writeSymbol(subscribe.getSymbol(), stream);
    }

    private Subscribe readSubscribe(DataInputStream stream) throws IOException {
        String symbol = readSymbol(stream);
        return Subscribe.builder()
            .symbol(symbol)
            .build();
    }

    private void writeUnsubscribe(Unsubscribe unsubscribe, DataOutputStream stream) throws IOException {
        writeSymbol(unsubscribe.getSymbol(), stream);
    }

    private Unsubscribe readUnsubscribe(DataInputStream stream) throws IOException {
        String symbol = readSymbol(stream);
        return Unsubscribe.builder()
            .symbol(symbol)
            .build();
    }


    private void writeQuote(Quote quote, DataOutputStream stream) throws IOException {
        writeSymbol(quote.getSymbol(), stream);
        stream.writeLong(Instant.now().toEpochMilli());
        stream.writeLong(quote.getVolume());
        stream.writeLong(quote.getPrice());
    }

    private void writeSymbol(String symbol, DataOutputStream stream) throws IOException {
        assert symbol.length() == Quote.SYMBOL_LENGTH;
        stream.write(symbol.getBytes(StandardCharsets.US_ASCII));
    }

    private Quote readQuote(DataInputStream stream) throws IOException {
        String symbol = readSymbol(stream);
        long volume = stream.readLong();
        long price = stream.readLong();
        long time = stream.readLong();
        return Quote.builder()
            .date(Instant.ofEpochMilli(time))
            .symbol(symbol)
            .volume(volume)
            .price(price)
            .build();
    }

    private String readSymbol(DataInputStream stream) throws IOException {
        byte[] bytes = new byte[Quote.SYMBOL_LENGTH];
        int length = stream.read(bytes);
        assert length == Quote.SYMBOL_LENGTH;
        return new String(bytes, StandardCharsets.US_ASCII);
    }
}

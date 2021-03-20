package segeraroot.quotemodel;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class Serialization {
    public void write(Quote quote, DataOutputStream stream) throws IOException {
        assert quote.getSymbol().length() == Quote.SYMBOL_LENGTH;
        stream.write(quote.getSymbol().getBytes(StandardCharsets.US_ASCII));
        stream.writeLong(Instant.now().toEpochMilli());
        stream.writeLong(quote.getVolume());
        stream.writeLong(quote.getPrice());
    }

    public Quote readQuote(DataInputStream stream) throws IOException {
        byte[] bytes = new byte[Quote.SYMBOL_LENGTH];
        int length = stream.read(bytes);
        assert length == Quote.SYMBOL_LENGTH;
        String symbol = new String(bytes, StandardCharsets.US_ASCII);
        long time = stream.readLong();
        long volume = stream.readLong();
        long price = stream.readLong();
        return Quote.builder()
                .date(Instant.ofEpochMilli(time))
                .symbol(symbol)
                .volume(volume)
                .price(price)
                .build();
    }
}

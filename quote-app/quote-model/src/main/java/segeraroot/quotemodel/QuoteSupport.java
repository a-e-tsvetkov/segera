package segeraroot.quotemodel;

import lombok.experimental.UtilityClass;

import java.nio.charset.StandardCharsets;

@UtilityClass
public class QuoteSupport {
    public byte[] convert(String symbol) {
        return symbol.getBytes(StandardCharsets.US_ASCII);
    }

    public String convert(byte[] bytes) {
        return new String(bytes, StandardCharsets.US_ASCII);
    }
}

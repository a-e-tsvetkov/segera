package segeraroot.quotemodel;

import java.nio.charset.StandardCharsets;

public class QuoteSupport {
    public static byte[] convert(String symbol) {
        return symbol.getBytes(StandardCharsets.US_ASCII);
    }

    public static String convert(byte[] bytes) {
        return new String(bytes, StandardCharsets.US_ASCII);
    }
}

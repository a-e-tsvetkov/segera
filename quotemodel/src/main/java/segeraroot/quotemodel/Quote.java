package segeraroot.quotemodel;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class Quote {
    public static final int SYMBOL_LENGTH = 3;
    Instant date;
    String symbol;
    long volume;
    long price;
}

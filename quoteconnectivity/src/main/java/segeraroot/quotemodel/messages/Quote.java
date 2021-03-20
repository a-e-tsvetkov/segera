package segeraroot.quotemodel.messages;

import lombok.Builder;
import lombok.Value;
import segeraroot.quotemodel.Message;

import java.time.Instant;

@Value
@Builder
public class Quote implements Message {
    public static final int SYMBOL_LENGTH = 3;
    Instant date;
    String symbol;
    long volume;
    long price;
}

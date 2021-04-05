package segeraroot.quoteconsumer.messages;

import lombok.Builder;
import lombok.Value;
import segeraroot.quoteconsumer.Message;

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

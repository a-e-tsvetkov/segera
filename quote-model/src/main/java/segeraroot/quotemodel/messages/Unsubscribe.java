package segeraroot.quotemodel.messages;

import lombok.Builder;
import lombok.Value;
import segeraroot.quotemodel.Message;

@Value
@Builder
public class Unsubscribe implements Message {
    String symbol;
}

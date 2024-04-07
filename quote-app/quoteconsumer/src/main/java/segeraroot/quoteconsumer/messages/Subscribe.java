package segeraroot.quoteconsumer.messages;

import lombok.Builder;
import lombok.Value;
import segeraroot.quoteconsumer.Message;

@Value
@Builder
public class Subscribe implements Message {
    String symbol;
}


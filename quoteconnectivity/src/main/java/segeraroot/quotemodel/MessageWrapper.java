package segeraroot.quotemodel;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MessageWrapper<T extends Message> {
    MessageType type;
    T value;
}

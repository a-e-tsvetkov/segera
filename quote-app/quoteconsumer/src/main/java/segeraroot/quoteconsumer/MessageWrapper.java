package segeraroot.quoteconsumer;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MessageWrapper {
    MessageType type;
    Message value;
}

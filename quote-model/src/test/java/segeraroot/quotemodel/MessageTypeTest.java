package segeraroot.quotemodel;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MessageTypeTest {

    @Test
    void values() {
        for (MessageType messageType : MessageType.values()) {
            Assertions.assertSame(messageType, MessageType.valueOf(messageType.getCode()));
        }
    }
}
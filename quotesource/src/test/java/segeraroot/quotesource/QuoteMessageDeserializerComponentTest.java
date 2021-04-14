package segeraroot.quotesource;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuoteMessageDeserializerComponentTest {

    @Test
    void longDeserializerOnMessage() {
        QuoteMessageDeserializer.LongDeserializer deserializer = new QuoteMessageDeserializer.LongDeserializer();

        ByteBuffer buffer = ByteBuffer.allocate(8);
        long value = 1234567890L;
        buffer.putLong(value);
        buffer.flip();
        boolean res = deserializer.onMessage(buffer);
        assertTrue(res);
        assertEquals(value, deserializer.getValue());
    }
}
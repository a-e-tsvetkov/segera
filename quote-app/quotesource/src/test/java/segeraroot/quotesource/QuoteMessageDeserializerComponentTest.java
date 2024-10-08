package segeraroot.quotesource;

import org.junit.jupiter.api.Test;
import segeraroot.connectivity.impl.deserializers.LongDeserializer;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuoteMessageDeserializerComponentTest {

    @Test
    void longDeserializerOnMessage() {
        LongDeserializer deserializer = new LongDeserializer();

        ByteBuffer buffer = ByteBuffer.allocate(8);
        long value = 1234567890L;
        buffer.putLong(value);
        buffer.flip();
        boolean res = deserializer.onMessage(buffer);
        assertTrue(res);
        assertEquals(value, deserializer.getValue());
    }
}

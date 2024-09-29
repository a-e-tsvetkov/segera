package segeraroot.connectivity.impl.deserializers;

import lombok.Getter;

import java.nio.ByteBuffer;

public class LongDeserializer {
    private int position;
    @Getter
    private long value;

    public boolean onMessage(ByteBuffer buffer) {
        while (buffer.hasRemaining()) {
            byte b = buffer.get();
            value = value << 8;
            value |= b & 0xffL;
            position++;
            if (position == 8) {
                return true;
            }
        }
        return false;
    }

    public void reset() {
        position = 0;
    }
}

package segeraroot.connectivity.util;

import java.nio.ByteBuffer;

public class LongDeserializer {
    private int position;
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

    public long getValue() {
        return value;
    }

    public void reset() {
        position = 0;
    }
}

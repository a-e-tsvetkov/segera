package segeraroot.connectivity.util;

import java.nio.ByteBuffer;

public class ByteDeserializer {
    private int value;

    public boolean onMessage(ByteBuffer buffer) {
        if (buffer.hasRemaining()) {
            value = buffer.get();
            return true;
        }
        return false;
    }

    public int getValue() {
        return value;
    }

    public void reset() {
    }
}

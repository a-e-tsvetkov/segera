package segeraroot.connectivity.impl.deserializers;

import lombok.Getter;

import java.nio.ByteBuffer;

public class IntDeserializer {
    private int position;
    @Getter
    private int value;

    public boolean onMessage(ByteBuffer buffer) {
        while (buffer.hasRemaining()) {
            byte b = buffer.get();
            value = value << 8;
            value |= b & 0xff;
            position++;
            if (position == 4) {
                return true;
            }
        }
        return false;
    }

    public void reset() {
        position = 0;
    }
}

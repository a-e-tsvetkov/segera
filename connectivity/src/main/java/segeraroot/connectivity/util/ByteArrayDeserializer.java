package segeraroot.connectivity.util;

import java.nio.ByteBuffer;

public class ByteArrayDeserializer {
    private final byte[] bytes;
    private int position = 0;

    public ByteArrayDeserializer(int symbolLength) {
        bytes = new byte[symbolLength];
    }

    public boolean onMessage(ByteBuffer buffer) {
        int length = Math.min(bytes.length - position, buffer.remaining());
        buffer.get(bytes, position, length);
        position += length;
        return position == bytes.length;
    }

    public byte[] getValue() {
        return bytes;
    }

    public void reset() {
        position = 0;
    }
}

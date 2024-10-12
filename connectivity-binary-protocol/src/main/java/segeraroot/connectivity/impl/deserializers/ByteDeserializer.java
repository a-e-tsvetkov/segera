package segeraroot.connectivity.impl.deserializers;

import lombok.Getter;

import java.nio.ByteBuffer;

@Getter
public class ByteDeserializer {
    private int value;

    public boolean onMessage(ByteBuffer buffer) {
        if (buffer.hasRemaining()) {
            value = buffer.get();
            return true;
        }
        return false;
    }

    @SuppressWarnings({"unused", "EmptyMethod"})
    public void reset() {
    }
}

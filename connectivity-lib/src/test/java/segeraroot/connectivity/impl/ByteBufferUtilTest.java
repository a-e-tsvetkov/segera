package segeraroot.connectivity.impl;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ByteBufferUtilTest {

    @Test
    void copy() {
        ByteBuffer src = ByteBuffer.allocate(3);
        ByteBuffer dst = ByteBuffer.allocate(2);
        src.put((byte) 1);
        src.put((byte) 2);
        src.put((byte) 3);
        src.flip();
        src.get();
        dst.put((byte) 4);

        ByteBufferUtil.copy(src, dst);
        dst.flip();

        assertEquals(4, dst.get());
        assertEquals(2, dst.get());
    }
}
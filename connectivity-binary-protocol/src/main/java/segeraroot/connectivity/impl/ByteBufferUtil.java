package segeraroot.connectivity.impl;

import java.nio.ByteBuffer;

public class ByteBufferUtil {
    public static void copy(ByteBuffer src, ByteBuffer dst) {
        int toCopy = Math.min(src.remaining(), dst.remaining());
        int oldLimit = src.limit();
        src.limit(src.position() + toCopy);
        dst.put(src);
        src.limit(oldLimit);
    }
}

package segeraroot.connectivity.http.impl;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class LineWriter {
    public static final byte[] EMPTY = new byte[0];
    private volatile byte[] content = EMPTY;
    private int index = 0;

    public void set(String content) {
        this.content = content.getBytes(StandardCharsets.US_ASCII);
        index = 0;
    }

    public boolean write(ByteBuffer buffer) {
        int length = Math.min(buffer.remaining(), content.length - index);
        buffer.put(content, index, length);
        index += length;
        return index == content.length;
    }
}

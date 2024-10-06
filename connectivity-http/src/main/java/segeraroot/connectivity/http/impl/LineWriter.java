package segeraroot.connectivity.http.impl;

import lombok.extern.slf4j.Slf4j;
import segeraroot.connectivity.callbacks.OperationResult;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Slf4j
public class LineWriter {
    public static final byte[] EMPTY = new byte[0];
    private volatile byte[] content = EMPTY;
    private int index = 0;

    public void set(String content) {
        this.content = content.getBytes(StandardCharsets.US_ASCII);
        index = 0;
    }

    public OperationResult write(ByteBuffer buffer) {
        int length = Math.min(buffer.remaining(), content.length - index);
        log.trace("write: {}", new String(content, index, length));
        buffer.put(content, index, length);
        index += length;
        return OperationResult.isDone(index == content.length);
    }
}

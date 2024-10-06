package segeraroot.connectivity.http.impl;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import segeraroot.connectivity.callbacks.OperationResult;

@Slf4j
public class HeaderDecoder {
    private final StringBuilder builder = new StringBuilder();
    @Getter
    private volatile String key;
    @Getter
    private volatile String value;
    @Getter
    private volatile boolean isDone;

    public OperationResult onMessage(ByteStream byteStream) {
        if (byteStream.readLine(builder) == OperationResult.CONTINUE) {
            return OperationResult.CONTINUE;
        }
        String string = builder.toString();
        if (string.isBlank()) {
            isDone = true;
            return OperationResult.DONE;
        }
        int index = string.indexOf(":");
        key = string.substring(0, index);
        value = string.substring(index + 1).trim();
        isDone = false;
        builder.setLength(0);
        return OperationResult.DONE;
    }
}

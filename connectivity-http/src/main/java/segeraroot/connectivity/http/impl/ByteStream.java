package segeraroot.connectivity.http.impl;

import segeraroot.connectivity.callbacks.ConnectivityChanel;
import segeraroot.connectivity.callbacks.OperationResult;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ByteStream {

    private final byte[] byteArray;
    private final ByteBuffer buffer;
    private int remaining;
    private int start;

    public ByteStream(int size) {
        this.byteArray = new byte[size];
        this.buffer = ByteBuffer.wrap(byteArray);
    }

    public void readFrom(ConnectivityChanel channel) throws IOException {
        buffer.position(0);
        buffer.limit(buffer.capacity());
        channel.read(buffer);
        buffer.flip();

        remaining = buffer.remaining();
        start = 0;
        buffer.get(byteArray, 0, remaining);
        assert !buffer.hasRemaining();
    }

    public OperationResult readLine(StringBuilder builder) {
        for (; start < remaining; start++) {
            char ch = (char) byteArray[start];
            switch (ch) {
                case '\r':
                    continue;
                case '\n':
                    start++;
                    return OperationResult.DONE;
            }
            builder.append(ch);
        }
        return OperationResult.CONTINUE;
    }

    public boolean hasRemaining() {
        return start != remaining;
    }

    public ByteBuffer byteBuffer() {
        return buffer;
    }
}

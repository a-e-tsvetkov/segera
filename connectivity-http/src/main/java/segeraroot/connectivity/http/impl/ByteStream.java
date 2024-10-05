package segeraroot.connectivity.http.impl;

import java.nio.ByteBuffer;

public class ByteStream {

    private final byte[] byteArray;
    private int remaining;
    private int start;

    public ByteStream(int size) {
        this.byteArray = new byte[size];
    }

    public void copy(ByteBuffer buffer) {
        remaining = buffer.remaining();
        start = 0;
        buffer.get(byteArray, 0, remaining);
    }

    public boolean readLine(StringBuilder builder) {
        for (; start < remaining; start++) {
            char ch = (char) byteArray[start];
            switch (ch) {
                case '\r':
                    continue;
                case '\n':
                    start++;
                    return true;
            }
            builder.append(ch);
        }
        return false;
    }

    public boolean hasRemaining() {
        return start != remaining;
    }
}

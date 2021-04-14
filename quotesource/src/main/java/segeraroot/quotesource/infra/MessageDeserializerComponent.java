package segeraroot.quotesource.infra;

import java.nio.ByteBuffer;

public interface MessageDeserializerComponent<T> {
    boolean onMessage(ByteBuffer buffer);

    default void reset() {
    }
}


package segeraroot.quotesource.infra;

import java.nio.ByteBuffer;

public interface MessageDeserializerComponent<T> {
    boolean onMessage(ByteBuffer buffer);

    T getValue();

    default void reset() {
    }
}


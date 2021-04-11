package segeraroot.quotesource.infra;

import java.nio.ByteBuffer;

public interface MessageSerializer<T> {
    ByteBuffer serialize(T message);
}

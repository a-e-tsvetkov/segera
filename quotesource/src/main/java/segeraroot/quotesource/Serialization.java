package segeraroot.quotesource;

import java.nio.ByteBuffer;

public interface Serialization<T> {
    ByteBuffer serialize(T message);
}

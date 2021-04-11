package segeraroot.connectivity;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public interface ByteBufferFactory {

    void write(Consumer<ByteBuffer> consumer);
}

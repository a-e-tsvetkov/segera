package segeraroot.connectivity.callbacks;

import java.nio.ByteBuffer;

public interface WriteCallback {
    boolean tryWrite(ByteBuffer buffer);
}

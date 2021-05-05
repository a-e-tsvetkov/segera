package segeraroot.connectivity.util;

import java.nio.ByteBuffer;

public interface WriteCallback {
    boolean tryWrite(ByteBuffer buffer);
}

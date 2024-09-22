package segeraroot.connectivity;

import java.nio.ByteBuffer;

public interface ReaderCallback {
    void onMessage(Connection connection, ByteBuffer buffer);
}

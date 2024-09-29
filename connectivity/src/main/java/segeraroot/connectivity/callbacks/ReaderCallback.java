package segeraroot.connectivity.callbacks;

import segeraroot.connectivity.Connection;

import java.nio.ByteBuffer;

public interface ReaderCallback {
    void onMessage(Connection connection, ByteBuffer buffer);
}

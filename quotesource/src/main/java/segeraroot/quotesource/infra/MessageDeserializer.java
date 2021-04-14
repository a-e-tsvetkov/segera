package segeraroot.quotesource.infra;

import segeraroot.connectivity.Connection;

import java.nio.ByteBuffer;

public interface MessageDeserializer {
    void onMessage(Connection connection, ByteBuffer buffer);

}

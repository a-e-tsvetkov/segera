package segeraroot.connectivity.callbacks;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface ConnectivityChanel{
    void read(ByteBuffer buffer) throws IOException;

    void write(ByteBuffer buffer) throws IOException;

    void stop();
}

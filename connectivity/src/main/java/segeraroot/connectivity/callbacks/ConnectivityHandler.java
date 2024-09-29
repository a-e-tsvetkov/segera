package segeraroot.connectivity.callbacks;

import segeraroot.connectivity.Connection;

import java.io.IOException;

public interface ConnectivityHandler extends ConnectionListener {
    void read(ConnectivityChanel channel, Connection connection) throws IOException;

    WritingResult write(ConnectivityChanel chanel, Connection connection) throws IOException;
}

package segeraroot.connectivity.callbacks;

import segeraroot.connectivity.Connection;

public interface ConnectionListener {
    void handleCloseConnection(Connection connection);

    Object handleNewConnection(Connection connection);
}

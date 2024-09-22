package segeraroot.connectivity;

public interface ConnectionListener {
    void handleCloseConnection(Connection connection);

    Object handleNewConnection(Connection connection);
}

package segeraroot.connectivity;

public interface ConnectionCallback<T> {
    void handleCloseConnection(Connection<T> connection);

    void handleNewConnection(Connection<T> connection);

    void handleMessageConnection(Connection<T> connection, T messageWrapper);
}

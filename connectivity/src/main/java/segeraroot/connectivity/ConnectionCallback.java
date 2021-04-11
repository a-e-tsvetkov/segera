package segeraroot.connectivity;

public interface ConnectionCallback<T, BuilderFactory> {
    void handleCloseConnection(Connection<T> connection);

    void handleNewConnection(Connection<T> connection);

    void handleMessage(Connection<T> connection, T message);

    void handleWriting(Connection<T> connection, BuilderFactory builderFactory);
}

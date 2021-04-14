package segeraroot.connectivity;

public interface ConnectionCallback<BuilderFactory> {
    void handleCloseConnection(Connection connection);

    void handleNewConnection(Connection connection);

    void handleWriting(Connection connection, BuilderFactory builderFactory);
}

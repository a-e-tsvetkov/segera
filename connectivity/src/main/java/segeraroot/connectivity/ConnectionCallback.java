package segeraroot.connectivity;

public interface ConnectionCallback<BuilderFactory> {
    void handleCloseConnection(Connection connection);

    void handleNewConnection(Connection connection);

    WritingResult handleWriting(Connection connection, BuilderFactory builderFactory);

    enum WritingResult {
        CONTINUE, DONE
    }
}

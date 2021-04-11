package segeraroot.connectivity;

import lombok.Builder;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Builder
public class SimpleConnectionCallback<T, BuilderFactory> implements ConnectionCallback<T, BuilderFactory> {
    private final Consumer<Connection<T>> closeHandler;
    private final Consumer<Connection<T>> newHandler;
    private final BiConsumer<Connection<T>, T> messageHandler;
    private final BiConsumer<Connection<T>, BuilderFactory> writingHandler;

    @Override
    public void handleCloseConnection(Connection<T> connection) {
        closeHandler.accept(connection);
    }

    @Override
    public void handleNewConnection(Connection<T> connection) {
        newHandler.accept(connection);
    }

    @Override
    public void handleMessage(Connection<T> connection, T messageWrapper) {
        messageHandler.accept(connection, messageWrapper);
    }

    @Override
    public void handleWriting(Connection<T> connection, BuilderFactory builderFactory) {
        writingHandler.accept(connection, builderFactory);
    }
}

package segeraroot.connectivity;

import lombok.Builder;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Builder
public class SimpleConnectionCallback<T> implements ConnectionCallback<T> {
    private final Consumer<Connection<T>> closeHandler;
    private final Consumer<Connection<T>> newHandler;
    private final BiConsumer<Connection<T>, T> messageHandler;

    @Override
    public void handleCloseConnection(Connection<T> connection) {
        closeHandler.accept(connection);
    }

    @Override
    public void handleNewConnection(Connection<T> connection) {
        newHandler.accept(connection);
    }

    @Override
    public void handleMessageConnection(Connection<T> connection, T messageWrapper) {
        messageHandler.accept(connection, messageWrapper);
    }
}

package segeraroot.quotemodel;

import lombok.Builder;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Builder
public class SimpleQuoteConnectionCallback<T> implements QuoteConnectionCallback<T> {
    private final Consumer<QuoteConnection<T>> closeHandler;
    private final Consumer<QuoteConnection<T>> newHandler;
    private final BiConsumer<QuoteConnection<T>, T> messageHandler;

    @Override
    public void handleCloseConnection(QuoteConnection<T> connection) {
        closeHandler.accept(connection);
    }

    @Override
    public void handleNewConnection(QuoteConnection<T> connection) {
        newHandler.accept(connection);
    }

    @Override
    public void handleMessageConnection(QuoteConnection<T> connection, T messageWrapper) {
        messageHandler.accept(connection, messageWrapper);
    }
}

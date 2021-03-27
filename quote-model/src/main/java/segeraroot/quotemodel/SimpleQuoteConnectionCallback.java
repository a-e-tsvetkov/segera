package segeraroot.quotemodel;

import lombok.Builder;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Builder
public class SimpleQuoteConnectionCallback implements QuoteConnectionCallback {
    private final Consumer<QuoteConnection> closeHandler;
    private final Consumer<QuoteConnection> newHandler;
    private final BiConsumer<QuoteConnection, MessageWrapper<?>> messageHandler;

    @Override
    public void handleCloseConnection(QuoteConnection connection) {
        closeHandler.accept(connection);
    }

    @Override
    public void handleNewConnection(QuoteConnection connection) {
        newHandler.accept(connection);
    }

    @Override
    public void handleMessageConnection(QuoteConnection connection, MessageWrapper<?> messageWrapper) {
        messageHandler.accept(connection, messageWrapper);
    }
}

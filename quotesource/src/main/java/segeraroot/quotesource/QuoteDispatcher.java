package segeraroot.quotesource;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import segeraroot.quotemodel.*;
import segeraroot.quotemodel.messages.Quote;
import segeraroot.quotemodel.messages.Subscribe;
import segeraroot.quotemodel.messages.Unsubscribe;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

@Slf4j
public class QuoteDispatcher {
    private final List<QuoteConnection<MessageWrapper>> connections = new ArrayList<>();
    private final Object connectionListLock = new Object();

    public void acceptQuote(Quote quote) {
        List<QuoteConnection<MessageWrapper>> list;
        synchronized (connectionListLock) {
            list = new ArrayList<>(connections);
        }
        for (var connection : list) {
            var context = getConnectionContext(connection);
            if (context.subscribed(quote.getSymbol())) {
                connection.write(MessageWrapper.builder()
                        .type(MessageType.QUOTE)
                        .value(quote)
                        .build());
            }
        }
    }

    private void onNewConnection(QuoteConnection<MessageWrapper> quoteConnection) {
        quoteConnection.set(new ConnectionContext(quoteConnection.getName()));
        synchronized (connectionListLock) {
            connections.add(quoteConnection);
        }
    }

    private void onCloseConnection(QuoteConnection<MessageWrapper> connectionHandler) {
        synchronized (connectionListLock) {
            connections.remove(connectionHandler);
        }
    }

    private void onMessage(QuoteConnection<MessageWrapper> connection, MessageWrapper messageWrapper) {
        var context = getConnectionContext(connection);
        switch (messageWrapper.getType()) {
            case SUBSCRIBE:
                subscribe(context, (Subscribe) messageWrapper.getValue());
                break;
            case UNSUBSCRIBE:
                unsubscribe(context, (Unsubscribe) messageWrapper.getValue());
                break;
            default:
                log.error("Unexpected message {}", messageWrapper.getType());
                break;
        }
    }

    private ConnectionContext getConnectionContext(QuoteConnection<MessageWrapper> connection) {
        return (ConnectionContext) connection.get();
    }

    private void subscribe(ConnectionContext context, Subscribe value) {
        log.debug("Subscribe: {} {}", context.getName(), value.getSymbol());
        context.subscribe(value.getSymbol());
    }

    private void unsubscribe(ConnectionContext context, Unsubscribe value) {
        log.debug("unsubscribe: {} {}", context.getName(), value.getSymbol());
        context.unsubscribe(value.getSymbol());
    }

    public QuoteConnectionCallback<MessageWrapper> getCallback() {
        return SimpleQuoteConnectionCallback.<MessageWrapper>builder()
                .newHandler(this::onNewConnection)
                .closeHandler(this::onCloseConnection)
                .messageHandler(this::onMessage)
                .build();
    }

    @RequiredArgsConstructor
    private static class ConnectionContext {
        private final Set<String> subscriptions = new ConcurrentSkipListSet<>();
        @Getter
        private final String name;

        public void subscribe(String symbol) {
            subscriptions.add(symbol);
        }

        public void unsubscribe(String symbol) {
            subscriptions.remove(symbol);
        }

        public boolean subscribed(String symbol) {
            return subscriptions.contains(symbol);
        }
    }
}

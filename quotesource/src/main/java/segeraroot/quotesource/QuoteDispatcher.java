package segeraroot.quotesource;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import segeraroot.connectivity.QuoteConnection;
import segeraroot.connectivity.QuoteConnectionCallback;
import segeraroot.connectivity.SimpleQuoteConnectionCallback;
import segeraroot.quotemodel.Message;
import segeraroot.quotemodel.QuoteSupport;
import segeraroot.quotemodel.messages.Quote;
import segeraroot.quotemodel.messages.Subscribe;
import segeraroot.quotemodel.messages.Unsubscribe;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

@Slf4j
public class QuoteDispatcher {
    private final List<QuoteConnection<Message>> connections = new ArrayList<>();
    private final Object connectionListLock = new Object();

    public void acceptQuote(Quote quote) {
        List<QuoteConnection<Message>> list;
        synchronized (connectionListLock) {
            list = new ArrayList<>(connections);
        }
        for (var connection : list) {
            var context = getConnectionContext(connection);
            if (context.subscribed(QuoteSupport.convert(quote.symbol()))) {
                connection.write(quote);
            }
        }
    }

    private void onNewConnection(QuoteConnection<Message> quoteConnection) {
        quoteConnection.set(new ConnectionContext(quoteConnection.getName()));
        synchronized (connectionListLock) {
            connections.add(quoteConnection);
        }
    }

    private void onCloseConnection(QuoteConnection<Message> connectionHandler) {
        synchronized (connectionListLock) {
            connections.remove(connectionHandler);
        }
    }

    private void onMessage(QuoteConnection<Message> connection, Message message) {
        var context = getConnectionContext(connection);
        switch (message.messageType()) {
            case Subscribe:
                subscribe(context, (Subscribe) message);
                break;
            case Unsubscribe:
                unsubscribe(context, (Unsubscribe) message);
                break;
            default:
                log.error("Unexpected message {}", message);
                break;
        }
    }

    private ConnectionContext getConnectionContext(QuoteConnection<Message> connection) {
        return (ConnectionContext) connection.get();
    }

    private void subscribe(ConnectionContext context, Subscribe value) {
        String symbol = QuoteSupport.convert(value.symbol());
        log.debug("Subscribe: {} {}", context.getName(), symbol);
        context.subscribe(symbol);
    }

    private void unsubscribe(ConnectionContext context, Unsubscribe value) {
        String symbol = QuoteSupport.convert(value.symbol());
        log.debug("unsubscribe: {} {}", context.getName(), symbol);
        context.unsubscribe(symbol);
    }

    public QuoteConnectionCallback<Message> getCallback() {
        return SimpleQuoteConnectionCallback.<Message>builder()
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

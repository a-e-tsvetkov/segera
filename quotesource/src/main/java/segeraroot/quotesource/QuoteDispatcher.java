package segeraroot.quotesource;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import segeraroot.connectivity.Connection;
import segeraroot.connectivity.ConnectionCallback;
import segeraroot.connectivity.SimpleConnectionCallback;
import segeraroot.quotemodel.BuilderFactory;
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
    private final List<Connection<Message>> connections = new ArrayList<>();
    private final Object connectionListLock = new Object();

    public void acceptQuote(Quote quote) {
        List<Connection<Message>> list;
        synchronized (connectionListLock) {
            list = new ArrayList<>(connections);
        }
        for (var connection : list) {
            var context = getConnectionContext(connection);
            if (context.subscribed(QuoteSupport.convert(quote.symbol()))) {
                context.add(quote);
                connection.startWriting();
            }
        }
    }

    private void onNewConnection(Connection<Message> connection) {
        connection.set(new ConnectionContext(connection.getName()));
        synchronized (connectionListLock) {
            connections.add(connection);
        }
    }

    private void onCloseConnection(Connection<Message> connectionHandler) {
        synchronized (connectionListLock) {
            connections.remove(connectionHandler);
        }
    }

    private void onMessage(Connection<Message> connection, Message message) {
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

    private ConnectionContext getConnectionContext(Connection<Message> connection) {
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

    public ConnectionCallback<Message, BuilderFactory> getCallback() {
        return SimpleConnectionCallback.<Message, BuilderFactory>builder()
                .newHandler(this::onNewConnection)
                .closeHandler(this::onCloseConnection)
                .messageHandler(this::onMessage)
                .writingHandler(this::onWrite)
                .build();
    }

    private void onWrite(Connection<Message> messageConnection, BuilderFactory builderFactory) {
        var context = getConnectionContext(messageConnection);
        context.drainQueue()
                .forEach(quote ->
                        builderFactory.createQuoteBuilder()
                                .symbol(quote.symbol())
                                .price(quote.price())
                                .volume(quote.volume())
                                .date(quote.date())
                                .send()
                );
    }

    @RequiredArgsConstructor
    private static class ConnectionContext {
        private final Set<String> subscriptions = new ConcurrentSkipListSet<>();
        private List<Quote> toSend = new ArrayList<>();
        private final Object lock = new Object();
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

        public List<Quote> drainQueue() {
            synchronized (lock) {
                List<Quote> tmp = this.toSend;
                toSend = new ArrayList<>();
                return tmp;
            }
        }

        public void add(Quote quote) {
            synchronized (lock) {
                toSend.add(quote);
            }
        }
    }
}

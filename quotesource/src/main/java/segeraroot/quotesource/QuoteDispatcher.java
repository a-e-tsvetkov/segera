package segeraroot.quotesource;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import segeraroot.connectivity.Connection;
import segeraroot.quotemodel.BuilderFactory;
import segeraroot.quotemodel.QuoteSupport;
import segeraroot.quotemodel.ReadersVisitor;
import segeraroot.quotemodel.messages.Quote;
import segeraroot.quotemodel.writers.SubscribeReader;
import segeraroot.quotemodel.writers.UnsubscribeReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

@Slf4j
public class QuoteDispatcher<BuilderFactoryImpl extends BuilderFactory> implements ReadersVisitor<BuilderFactoryImpl> {
    private final List<Connection> connections = new ArrayList<>();
    private final Object connectionListLock = new Object();

    public void acceptQuote(Quote quote) {
        List<Connection> list;
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

    @Override
    public void handleCloseConnection(Connection connection) {
        onCloseConnection(connection);
    }

    @Override
    public void handleNewConnection(Connection connection) {
        onNewConnection(connection);
    }

    @Override
    public void handleWriting(Connection connection, BuilderFactory builderFactory) {
        onWrite(connection, builderFactory);
    }

    private void onNewConnection(Connection connection) {
        connection.set(new ConnectionContext(connection.getName()));
        synchronized (connectionListLock) {
            connections.add(connection);
        }
    }

    private void onCloseConnection(Connection connectionHandler) {
        synchronized (connectionListLock) {
            connections.remove(connectionHandler);
        }
    }

    @Override
    public void visit(Connection connection, SubscribeReader value) {
        var context = getConnectionContext(connection);
        String symbol = QuoteSupport.convert(value.symbol());
        log.debug("Subscribe: {} {}", context.getName(), symbol);
        context.subscribe(symbol);
    }

    @Override
    public void visit(Connection connection, UnsubscribeReader value) {
        var context = getConnectionContext(connection);
        String symbol = QuoteSupport.convert(value.symbol());
        log.debug("unsubscribe: {} {}", context.getName(), symbol);
        context.unsubscribe(symbol);
    }

    private ConnectionContext getConnectionContext(Connection connection) {
        return (ConnectionContext) connection.get();
    }

    private void onWrite(Connection messageConnection, BuilderFactory builderFactory) {
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

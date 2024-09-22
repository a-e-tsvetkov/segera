package segeraroot.quotesource;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import segeraroot.connectivity.Connection;
import segeraroot.connectivity.ConnectionListener;
import segeraroot.connectivity.WriterCallback;
import segeraroot.connectivity.WritingResult;
import segeraroot.quotemodel.BuilderFactory;
import segeraroot.quotemodel.QuoteSupport;
import segeraroot.quotemodel.ReadersVisitor;
import segeraroot.quotemodel.messages.Quote;
import segeraroot.quotemodel.writers.SubscribeReader;
import segeraroot.quotemodel.writers.UnsubscribeReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class QuoteDispatcher implements ConnectionListener, ReadersVisitor , WriterCallback<BuilderFactory> {
    private final List<Connection> connections = new ArrayList<>();
    private final Object connectionListLock = new Object();

    public void acceptQuote(Quote quote) {
        List<Connection> list;
        synchronized (connectionListLock) {
            list = new ArrayList<>(connections);
        }
        for (var connection : list) {
            ConnectionContext context = connection.get();
            if (context.subscribed(QuoteSupport.convert(quote.symbol()))) {
                context.add(quote);
                connection.startWriting();
            }
        }
    }

    @Override
    public void handleCloseConnection(Connection connection) {
        synchronized (connectionListLock) {
            connections.remove(connection);
        }
    }

    @Override
    public Object handleNewConnection(Connection connection) {
        synchronized (connectionListLock) {
            connections.add(connection);
        }
        return new ConnectionContext(connection.getName());
    }

    @Override
    public WritingResult handleWriting(Connection connection, BuilderFactory builderFactory) {
        ConnectionContext context = connection.get();
        Quote quote;
        while ((quote = context.peek()) != null) {
            boolean success = builderFactory.createQuote()
                    .symbol(quote.symbol())
                    .price(quote.price())
                    .volume(quote.volume())
                    .date(quote.date())
                    //TODO: Ensure buffer have enough space
                    .send();

            if (success) {
                context.poll();
            } else {
                return WritingResult.CONTINUE;
            }
        }
        return WritingResult.DONE;
    }

    @Override
    public void visit(Connection connection, SubscribeReader value) {
        ConnectionContext context = connection.get();
        String symbol = QuoteSupport.convert(value.symbol());
        log.debug("Subscribe: {} {}", context.getName(), symbol);
        context.subscribe(symbol);
    }

    @Override
    public void visit(Connection connection, UnsubscribeReader value) {
        ConnectionContext context = connection.get();
        String symbol = QuoteSupport.convert(value.symbol());
        log.debug("unsubscribe: {} {}", context.getName(), symbol);
        context.unsubscribe(symbol);
    }

    @RequiredArgsConstructor
    private static class ConnectionContext {
        private final Set<String> subscriptions = new ConcurrentSkipListSet<>();
        private final Queue<Quote> toSend = new LinkedBlockingQueue<>();
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

        public Quote peek() {
            return toSend.peek();
        }

        public Quote poll() {
            return toSend.poll();
        }

        public void add(Quote quote) {
            toSend.add(quote);
        }
    }
}

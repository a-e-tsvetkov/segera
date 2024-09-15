package segeraroot.quotesource;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import segeraroot.connectivity.Connection;
import segeraroot.performancecounter.PCCounter;
import segeraroot.performancecounter.PCHost;
import segeraroot.quotemodel.BuilderFactory;
import segeraroot.quotemodel.QuoteSupport;
import segeraroot.quotemodel.ReadersVisitor;
import segeraroot.quotemodel.writers.QuoteReader;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class QuoteConsumer<BuilderFactoryImpl extends BuilderFactory> implements ReadersVisitor<BuilderFactoryImpl> {
    private final List<String> symbols;
    private final PCCounter quoteCount;

    public QuoteConsumer(PCHost pcHost, String... symbols) {
        this.symbols = Arrays.asList(symbols);
        quoteCount = pcHost.counter("data.quote.count");
    }

    @Override
    public void handleCloseConnection(Connection connection) {
        var context = getConnectionContext(connection);
        context.setSubscribed(false);
    }

    @Override
    public void handleNewConnection(Connection connection) {
        connection.set(new ConnectionContext(connection.getName()));
        connection.startWriting();
    }

    @Override
    public WritingResult handleWriting(Connection connection, BuilderFactory builderFactory) {
        var context = getConnectionContext(connection);
        if (!context.isSubscribed()) {
            for (String quote : symbols) {
                boolean success = builderFactory.createSubscribe()
                        .symbol(QuoteSupport.convert(quote))
                        .send();
                if (!success) {
                    log.error("Failed to subscribe to {}", quote);
                }
            }
        }
        context.setSubscribed(true);
        //TODO: We are too optimistic here
        return WritingResult.DONE;
    }


    @Override
    public void visit(Connection connection, QuoteReader value) {
        quoteCount.add(1);
        log.info("Quote received: symbol={}, volume={}, price={}, date={}",
                QuoteSupport.convert(value.symbol()),
                value.volume(),
                value.price(),
                Instant.ofEpochMilli(value.date())

        );
    }

    private ConnectionContext getConnectionContext(Connection connection) {
        return (ConnectionContext) connection.get();
    }

    @RequiredArgsConstructor
    private static class ConnectionContext {
        @Getter
        private final String name;
        @Setter
        @Getter
        private volatile boolean subscribed;
    }
}

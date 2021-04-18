package segeraroot.quotesource;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import segeraroot.connectivity.Connection;
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

    public QuoteConsumer(String... symbols) {
        this.symbols = Arrays.asList(symbols);
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
    public void handleWriting(Connection connection, BuilderFactory builderFactory) {
        var context = getConnectionContext(connection);
        if (!context.isSubscribed()) {
            symbols.forEach(quote ->
                    builderFactory.createSubscribeBuilder()
                            .symbol(QuoteSupport.convert(quote))
                            .send()
            );
        }
        context.setSubscribed(true);
    }


    @Override
    public void visit(Connection connection, QuoteReader value) {
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

package segeraroot.quotesource;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import segeraroot.connectivity.Connection;
import segeraroot.connectivity.callbacks.ConnectionListener;
import segeraroot.connectivity.callbacks.WriterCallback;
import segeraroot.connectivity.callbacks.OperationResult;
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
public class QuoteConsumer implements ReadersVisitor, WriterCallback<BuilderFactory>, ConnectionListener {
    private final List<String> symbols;
    private final PCCounter quoteCount;

    public QuoteConsumer(PCHost pcHost, String... symbols) {
        this.symbols = Arrays.asList(symbols);
        quoteCount = pcHost.counter("data.quote.count");
    }

    @Override
    public void handleCloseConnection(Connection connection) {
        ConnectionContext context = connection.get();
        context.setSubscribed(false);
    }

    @Override
    public Object handleNewConnection(Connection connection) {
        connection.startWriting();
        return new ConnectionContext(connection.getName());
    }

    @Override
    public OperationResult handleWriting(Connection connection, BuilderFactory builderFactory) {
        ConnectionContext context = connection.get();
        if (!context.isSubscribed()) {
            for (String quote : symbols) {
                log.debug("Subscribing to {}", quote);
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
        return OperationResult.DONE;
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

    @Getter
    @RequiredArgsConstructor
    private static class ConnectionContext {
        private final String name;
        @Setter
        private volatile boolean subscribed;
    }
}

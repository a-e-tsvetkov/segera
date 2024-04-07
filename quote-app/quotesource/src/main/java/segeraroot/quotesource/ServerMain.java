package segeraroot.quotesource;

import segeraroot.connectivity.impl.SerializationConnectionCallbackFactory;
import segeraroot.connectivity.impl.Server;
import segeraroot.quotemodel.ReadersVisitor;
import segeraroot.quotemodel.impl.BuilderFactoryImpl;
import segeraroot.quotemodel.impl.MessageDeserializerImpl;

import java.util.Arrays;

public class ServerMain {
    public static void main(String[] args) {
        var quoteDispatcher = new QuoteDispatcher<BuilderFactoryImpl>();
        var serializer = new SerializationConnectionCallbackFactory<BuilderFactoryImpl, ReadersVisitor<BuilderFactoryImpl>>(
                MessageDeserializerImpl::new,
                BuilderFactoryImpl::new
        );

        for (String symbol : Arrays.asList("ABC", "DEF", "GHK")) {
            var quoteGenerator = new QuoteGenerator(symbol, quoteDispatcher::acceptQuote);
            quoteGenerator.start();
        }

        var server = new Server<>(9000, serializer.handleMessage(quoteDispatcher));
        server.start();

    }

}

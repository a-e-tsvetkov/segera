package segeraroot.quotesource;

import segeraroot.connectivity.ConnectionCallback;
import segeraroot.quotemodel.BuilderFactory;
import segeraroot.quotemodel.Message;
import segeraroot.quotesource.infra.SerializationConnectionCallbackFactory;
import segeraroot.quotesource.infra.Server;

import java.util.Arrays;

public class ServerMain {
    public static void main(String[] args) {
        var quoteDispatcher = new QuoteDispatcher();
        var serializer = new SerializationConnectionCallbackFactory<>(
                new QuoteMessageDeserializer(),
                BuilderFactoryImpl::new
        );

        for (String symbol : Arrays.asList("ABC", "DEF", "GHK")) {
            var quoteGenerator = new QuoteGenerator(symbol, quoteDispatcher::acceptQuote);
            quoteGenerator.start();
        }

        ConnectionCallback<Message, BuilderFactory> callback = quoteDispatcher.getCallback();
        var server = new Server(9000, serializer.handleMessage(callback));
        server.start();

    }

}

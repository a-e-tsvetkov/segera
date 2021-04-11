package segeraroot.quotesource;

import segeraroot.quotesource.infra.SerializationConnectionCallbackFactory;
import segeraroot.quotesource.infra.Server;

import java.util.Arrays;

public class ServerMain {
    public static void main(String[] args) {
        var quoteDispatcher = new QuoteDispatcher();
        var serializer = new SerializationConnectionCallbackFactory<>(
                new QuoteMessageSerializer(),
                new QuoteMessageDeserializer());

        for (String symbol : Arrays.asList("ABC", "DEF", "GHK")) {
            var quoteGenerator = new QuoteGenerator(symbol, quoteDispatcher::acceptQuote);
            quoteGenerator.start();
        }

        var server = new Server(9000, serializer.handleMessage(quoteDispatcher.getCallback()));
        server.start();

    }
}

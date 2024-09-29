package segeraroot.quotesource;

import segeraroot.quotemodel.Protocol;

import java.util.Arrays;

public class ServerMain {
    public static void main(String[] args) {
        var quoteDispatcher = new QuoteDispatcher();

        for (String symbol : Arrays.asList("ABC", "DEF", "GHK")) {
            var quoteGenerator = new QuoteGenerator(
                    symbol,
                    quoteDispatcher::acceptQuote,
                    Thread::new);
            quoteGenerator.start();
        }

        var server = Protocol.server(
                        quoteDispatcher,
                        quoteDispatcher,
                        quoteDispatcher
                ).at(9000);

        server.start();

    }

}

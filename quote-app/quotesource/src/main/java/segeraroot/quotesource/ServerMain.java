package segeraroot.quotesource;

import segeraroot.connectivity.impl.Server;
import segeraroot.quotemodel.ServerProtocol;

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

        var protocol = ServerProtocol.of(
                quoteDispatcher,
                quoteDispatcher,
                quoteDispatcher
        );
        var server = new Server(9000, protocol);
        server.start();

    }

}

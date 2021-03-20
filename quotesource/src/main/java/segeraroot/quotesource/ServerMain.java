package segeraroot.quotesource;

import java.util.Arrays;

public class ServerMain {
    public static void main(String[] args) {
        var quoteDispatcher = new QuoteDispatcher();

        for (String symbol : Arrays.asList("ABC", "DEF", "GHK")){
            var quoteGenerator = new QuoteGenerator(symbol, quoteDispatcher::acceptQuote);
            quoteGenerator.start();
        }

        var server = new Server(9000, quoteDispatcher.getCallback());
        server.start();

    }
}

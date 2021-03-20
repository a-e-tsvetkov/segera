package segeraroot.quotesource;

import java.util.Arrays;

public class ServerMain {
    public static void main(String[] args) {
        var server = new Server(9000);
        server.start();

        for (String symbol : Arrays.asList("ABC", "DEF", "GHK")){
            var quoteGenerator = new QuteGenerator(symbol, server::acceptQuote);
            quoteGenerator.start();
        }
    }
}

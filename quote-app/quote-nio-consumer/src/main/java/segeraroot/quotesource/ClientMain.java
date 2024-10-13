package segeraroot.quotesource;

import segeraroot.performancecounter.impl.PCHostImpl;
import segeraroot.quotemodel.Protocol;

public class ClientMain {
    public static void main(String[] args) {
        var pcHost = new PCHostImpl();
        var quoteDispatcher = new QuoteConsumer(pcHost, "ABC", "DEF");
        var client = Protocol.client(
            quoteDispatcher,
            quoteDispatcher,
            quoteDispatcher
        ).connectTo("localhost", 9000);
        client.start();
    }
}

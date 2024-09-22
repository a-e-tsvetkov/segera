package segeraroot.quotesource;

import segeraroot.connectivity.ProtocolInterface;
import segeraroot.connectivity.impl.Client;
import segeraroot.performancecounter.impl.PCHostImpl;

public class ClientMain {
    public static void main(String[] args) {
        var pcHost = new PCHostImpl();
        var quoteDispatcher = new QuoteConsumer(pcHost, "ABC", "DEF");

        ProtocolInterface protocol = ClientProtocol.of(
                quoteDispatcher,
                quoteDispatcher,
                quoteDispatcher
        );

        var server = new Client("localhost", 9000, protocol);
        server.start();

    }

}

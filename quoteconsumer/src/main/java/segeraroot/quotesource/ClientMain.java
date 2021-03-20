package segeraroot.quotesource;

public class ClientMain {
    public static void main(String[] args) {
        var terminal = new PriceTerminal();
        var client = new Client("localhost", 9000, terminal::acceptQuote);

        client.start();
    }
}

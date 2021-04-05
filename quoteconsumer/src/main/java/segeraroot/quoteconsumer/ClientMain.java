package segeraroot.quoteconsumer;

public class ClientMain {
    public static void main(String[] args) {
        var terminal = new PriceTerminal("ABC", "DEF");
        var client = new Client("localhost", 9000, terminal.getCallback());

        client.start();
    }
}

package segeraroot.quotesource;

public class ClientMain {
    public static void main(String[] args) {
        var server = new Client("localhost",9000);
        server.start();
    }
}

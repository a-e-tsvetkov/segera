package segeraroot.quotesource;

public class ServerMain {
    public static void main(String[] args) {
        var server = new Server(9000);
        server.start();
    }
}

package segeraroot.quotesource;

import lombok.extern.slf4j.Slf4j;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;


@Slf4j
public class Client {
    private final String host;
    private final int port;

    public Client(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() {
        awaitMessageLoop();
    }

    private void awaitMessageLoop() {
        try {
            log.info("Start server: host={} port={}", host, port);
            var socket = new Socket(host, port);
            ConnectionHandler connectionHandler = new ConnectionHandler(socket);
            connectionHandler.start();
        } catch (IOException e) {
            log.error("Exception in await loop", e);
        }
    }

    private void readQuote(DataInputStream stream) throws IOException {
        byte[] bytes = new byte[3];
        int length = stream.read(bytes);
        assert length == 3;
        String name = new String(bytes, StandardCharsets.US_ASCII);
        long time = stream.readLong();
        long volume = stream.readLong();
        long price = stream.readLong();

        log.info("new quote: {} {} {} {}", name, time, volume, price);
    }

    private class ConnectionHandler {

        private final Socket connection;
        private final DataInputStream outputStream;
        private volatile boolean running;

        public ConnectionHandler(Socket connection) throws IOException {
            this.connection = connection;
            this.outputStream = new DataInputStream(connection.getInputStream());
        }

        public void start() {
            Thread writeThread = new Thread(this::readQuotesLoop);
            running = true;
            writeThread.start();
        }

        public void stop() {
            running = false;
            try {
                connection.close();
            } catch (IOException e) {
                log.error("Failed to close connection", e);
            }
        }

        private void readQuotesLoop() {
            try {
                while (running) {
                    readQuote(outputStream);
                }
            } catch (IOException e) {
                log.error("Exception in write loop", e);
            }
        }
    }
}

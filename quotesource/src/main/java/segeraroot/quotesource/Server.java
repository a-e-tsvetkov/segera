package segeraroot.quotesource;

import lombok.extern.slf4j.Slf4j;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;


@Slf4j
public class Server {
    private final int port;

    public Server(int port) {
        this.port = port;
    }

    public void start() {
        Thread mainThread = new Thread(this::awaitConnectionsLoop);

        mainThread.start();
    }

    private void awaitConnectionsLoop() {
        try {
            log.info("Start server: port={}", port);
            ServerSocket serverSocket = new ServerSocket(port);
            while (true) {
                Socket connection = serverSocket.accept();
                log.debug("Connection accepted: {}", connection.getRemoteSocketAddress());
                startConnectionThread(connection);
            }
        } catch (IOException e) {
            log.error("Exception in await loop", e);
        }
    }

    private void startConnectionThread(Socket connection) throws IOException {
        ConnectionHandler connectionHandler = new ConnectionHandler(connection);
        connectionHandler.start();
    }

    private void writeQuote(DataOutputStream stream) throws IOException {
        stream.write("ABC".getBytes(StandardCharsets.US_ASCII));
        stream.writeLong(Instant.now().toEpochMilli());
        stream.writeLong(100);//volume
        stream.writeLong(6_00100);//price

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class ConnectionHandler {

        private final Socket connection;
        private final DataOutputStream outputStream;
        private Thread writeThread;
        private volatile boolean running;

        public ConnectionHandler(Socket connection) throws IOException {
            this.connection = connection;
            this.outputStream = new DataOutputStream(connection.getOutputStream());
        }

        public void start() {
            writeThread = new Thread(this::writeQuotesLoop);
            running = true;
            writeThread.start();

        }

        public void stop() {
            log.info("Close connection: {}", connection.getRemoteSocketAddress());
            running = false;
            try {
                connection.close();
            } catch (IOException e) {
                log.error("Fail to close connection", e);
            }
        }

        private void writeQuotesLoop() {
            try {
                while (running) {
                    writeQuote(outputStream);
                }
            } catch (SocketException e) {
                log.info("Connection closed: {}", e.getMessage());
                stop();
            } catch (IOException e) {
                log.error("Exception in write loop", e);
            }
        }
    }
}

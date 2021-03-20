package segeraroot.quotesource;

import lombok.extern.slf4j.Slf4j;
import segeraroot.quotemodel.Quote;
import segeraroot.quotemodel.Serialization;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;


@Slf4j
public class Server {
    private final int port;
    private final List<ConnectionHandler> connections = new ArrayList<>();
    private final Object connectionListLock = new Object();
    private final Serialization serialization = new Serialization();

    public Server(int port) {
        this.port = port;
    }

    public void start() {
        Thread mainThread = new Thread(this::awaitConnectionsLoop);

        mainThread.start();
    }

    public void acceptQuote(Quote quote) {
        List<ConnectionHandler> list;
        synchronized (connectionListLock) {
            list = new ArrayList<>(connections);
        }
        for (var connection : list) {
            connection.write(quote);
        }
    }

    private void awaitConnectionsLoop() {
        try {
            log.info("Start server: port={}", port);
            ServerSocket serverSocket = new ServerSocket(port);
            while (true) {
                Socket connection = serverSocket.accept();
                log.debug("Connection accepted: {}", connection.getRemoteSocketAddress());
                handleNewConnection(connection);
            }
        } catch (IOException e) {
            log.error("Exception in await loop", e);
        }
    }

    private void handleNewConnection(Socket connection) throws IOException {
        ConnectionHandler connectionHandler = new ConnectionHandler(connection);
        connectionHandler.start();
        synchronized (connectionListLock) {
            connections.add(connectionHandler);
        }
    }

    private void handleCloseConnection(ConnectionHandler connectionHandler) {
        synchronized (connectionListLock) {
            connections.remove(connectionHandler);
        }
    }

    private class ConnectionHandler {

        private final Socket connection;
        private final DataOutputStream outputStream;
        private final Object writeLock = new Object();

        public ConnectionHandler(Socket connection) throws IOException {
            this.connection = connection;
            this.outputStream = new DataOutputStream(connection.getOutputStream());
        }

        public void start() {
        }

        public void stop() {
            handleCloseConnection(this);
            try {
                connection.close();
            } catch (IOException e) {
                log.error("Error while closing socket: {}", e.getMessage());
            }
        }

        public void write(Quote quote) {
            synchronized (writeLock) {
                try {
                    serialization.write(quote, this.outputStream);
                    this.outputStream.flush();
                } catch (IOException e) {
                    log.error("Unable to write to socket: {}", e.getMessage());
                    stop();
                }
            }
        }
    }
}

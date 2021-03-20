package segeraroot.quotesource;

import lombok.extern.slf4j.Slf4j;
import segeraroot.quotemodel.ConnectionHandlerBase;
import segeraroot.quotemodel.MessageWrapper;
import segeraroot.quotemodel.QuoteConnectionCallback;
import segeraroot.quotemodel.Serialization;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


@Slf4j
public class Server {
    private final int port;
    private final QuoteConnectionCallback connectionCallback;
    private final Serialization serialization = new Serialization();

    public Server(int port, QuoteConnectionCallback connectionCallback) {
        this.port = port;
        this.connectionCallback = connectionCallback;
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
                handleNewConnection(connection);
            }
        } catch (IOException e) {
            log.error("Exception in await loop", e);
        }
    }

    private void handleNewConnection(Socket connection) throws IOException {
        ConnectionHandler connectionHandler = new ConnectionHandler(connection);
        connectionHandler.start();
        connectionCallback.handleNewConnection(connectionHandler);
    }

    private class ConnectionHandler extends ConnectionHandlerBase {

        public ConnectionHandler(Socket connection) throws IOException {
            super(connection);
        }

        @Override
        public void start() {
            writeHeader();
            super.start();
        }

        @Override
        protected void onMessage(MessageWrapper<?> messageWrapper) {
            connectionCallback.handleMessageConnection(this, messageWrapper);
        }

        @Override
        protected Serialization getSerialization() {
            return serialization;
        }

        @Override
        protected void handleCloseConnection() {
            connectionCallback.handleCloseConnection(this);
        }

    }
}

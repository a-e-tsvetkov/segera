package segeraroot.quoteconsumer;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.Socket;

@Slf4j
public class Client {
    private final String host;
    private final int port;
    private final QuoteConnectionCallback<MessageWrapper> callback;
    private final Serialization serialization = new Serialization();

    public Client(String host, int port, QuoteConnectionCallback<MessageWrapper> callback) {
        this.host = host;
        this.port = port;
        this.callback = callback;
    }

    public void start() {
        try {
            log.info("Start client: host={} port={}", host, port);
            var socket = new Socket(host, port);
            ConnectionHandler connectionHandler = new ConnectionHandler(socket);
            connectionHandler.start();
            callback.handleNewConnection(connectionHandler);
        } catch (IOException e) {
            log.error("Exception in await loop", e);
        }
    }

    private class ConnectionHandler extends ConnectionHandlerBase implements QuoteConnection<MessageWrapper> {
        public ConnectionHandler(Socket connection) throws IOException {
            super(connection);
        }

        @Override
        public void start() {
            readHeader();
            super.start();
        }

        @Override
        protected void handleCloseConnection() {

        }


        @Override
        protected void onMessage(MessageWrapper messageWrapper) {
            callback.handleMessageConnection(this, messageWrapper);
        }

        @Override
        protected Serialization getSerialization() {
            return serialization;
        }
    }
}

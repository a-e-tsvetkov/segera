package segeraroot.quotesource;

import lombok.extern.slf4j.Slf4j;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.function.Consumer;
import segeraroot.quotemodel.Quote;
import segeraroot.quotemodel.Serialization;

@Slf4j
public class Client {
    private final String host;
    private final int port;
    private final Consumer<Quote> quoteConsumer;
    private final Serialization serialization = new Serialization();

    public Client(String host, int port, Consumer<Quote> quoteConsumer) {
        this.host = host;
        this.port = port;
        this.quoteConsumer = quoteConsumer;
    }

    public void start() {
        try {
            log.info("Start client: host={} port={}", host, port);
            var socket = new Socket(host, port);
            ConnectionHandler connectionHandler = new ConnectionHandler(socket);
            connectionHandler.start();
        } catch (IOException e) {
            log.error("Exception in await loop", e);
        }
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
            Thread thread = new Thread(this::readQuotesLoop);
            running = true;
            thread.start();
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
                while (running) {
                    Quote quote;
                    try {
                        quote = serialization.readQuote(outputStream);
                    } catch (IOException e) {
                        log.error("Exception in read loop", e);
                        stop();
                        continue;
                    }
                    try {
                        quoteConsumer.accept(quote);
                    } catch (RuntimeException e) {
                        log.error("Failed to process quote: " + quote, e);
                    } catch (Throwable e) {
                        log.error("ERROR DURING PROCESSING: " + quote, e);
                        stop();
                    }
                }
        }
    }
}

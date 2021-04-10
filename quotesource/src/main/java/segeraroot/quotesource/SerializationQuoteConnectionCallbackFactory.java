package segeraroot.quotesource;

import lombok.AllArgsConstructor;
import segeraroot.connectivity.QuoteConnection;
import segeraroot.connectivity.QuoteConnectionCallback;
import segeraroot.connectivity.SimpleQuoteConnectionCallback;
import segeraroot.quotemodel.Message;

import java.nio.ByteBuffer;

@AllArgsConstructor
public class SerializationQuoteConnectionCallbackFactory {
    private Serialization serialization;

    public QuoteConnectionCallback<ByteBuffer> handleMessage(QuoteConnectionCallback<Message> callback) {
        return SimpleQuoteConnectionCallback.<ByteBuffer>builder()
                .newHandler(connection ->
                        callback.handleNewConnection(attach(callback, connection)))
                .closeHandler(connection ->
                        callback.handleCloseConnection(getConnection(connection)))
                .messageHandler(this::handleMessage)
                .build();
    }

    private QuoteConnection<Message> attach(QuoteConnectionCallback<Message> callback, QuoteConnection<ByteBuffer> connection) {
        var wrapper = new SerializingConnection(connection, callback, serialization, "W|" + connection.getName());
        connection.set(wrapper);
        return wrapper;
    }

    private void handleMessage(QuoteConnection<ByteBuffer> connection, ByteBuffer buffer) {
        var wrapper = (SerializingConnection) connection.get();
        wrapper.onMessage(buffer);
    }

    private QuoteConnection<Message> getConnection(QuoteConnection<ByteBuffer> connection) {
        return (SerializingConnection) connection.get();
    }


    private static class SerializingConnection
            extends QuoteConnectionBase<Message>
            implements MessageSink<Message> {
        private final QuoteConnection<ByteBuffer> connection;
        private final QuoteConnectionCallback<Message> callback;
        private final Serialization serialization;
        private final String name;

        public SerializingConnection(
                QuoteConnection<ByteBuffer> connection,
                QuoteConnectionCallback<Message> callback,
                Serialization serialization,
                String name) {
            this.connection = connection;
            this.callback = callback;
            this.serialization = serialization;
            this.name = name;
        }

        @Override
        public void write(Message messageWrapper) {
            connection.write(serialization.serialize(messageWrapper));
        }

        @Override
        public String getName() {
            return name;
        }

        public void onMessage(ByteBuffer buffer) {
            serialization.onMessage(buffer, this);
        }

        @Override
        public void send(Message message) {
            callback.handleMessageConnection(SerializingConnection.this, message);
        }
    }
}

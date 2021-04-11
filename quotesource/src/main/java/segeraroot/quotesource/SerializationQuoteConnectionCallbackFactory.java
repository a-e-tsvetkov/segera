package segeraroot.quotesource;

import lombok.AllArgsConstructor;
import segeraroot.connectivity.QuoteConnection;
import segeraroot.connectivity.QuoteConnectionCallback;
import segeraroot.connectivity.SimpleQuoteConnectionCallback;

import java.nio.ByteBuffer;

@AllArgsConstructor
public class SerializationQuoteConnectionCallbackFactory<T> {
    private final Serialization<T> serialization;
    private final MessageDeserializer<T> deserializer;

    public QuoteConnectionCallback<ByteBuffer> handleMessage(QuoteConnectionCallback<T> callback) {
        return SimpleQuoteConnectionCallback.<ByteBuffer>builder()
                .newHandler(connection ->
                        callback.handleNewConnection(attach(callback, connection)))
                .closeHandler(connection ->
                        callback.handleCloseConnection(getConnection(connection)))
                .messageHandler(this::handleMessage)
                .build();
    }

    private QuoteConnection<T> attach(QuoteConnectionCallback<T> callback, QuoteConnection<ByteBuffer> connection) {
        var wrapper = new SerializingConnection<>(
                connection,
                callback,
                serialization,
                deserializer,
                "W|" + connection.getName());
        connection.set(wrapper);
        return wrapper;
    }

    private void handleMessage(QuoteConnection<ByteBuffer> connection, ByteBuffer buffer) {
        var wrapper = getConnection(connection);
        wrapper.onMessage(buffer);
    }

    @SuppressWarnings("unchecked")
    private SerializingConnection<T> getConnection(QuoteConnection<ByteBuffer> connection) {
        return (SerializingConnection<T>) connection.get();
    }


    private static class SerializingConnection<T>
            extends QuoteConnectionBase<T> {
        private final QuoteConnection<ByteBuffer> connection;
        private final QuoteConnectionCallback<T> callback;
        private final Serialization<T> serialization;
        private final MessageDeserializer<T> deserializer;

        private final String name;

        public SerializingConnection(
                QuoteConnection<ByteBuffer> connection,
                QuoteConnectionCallback<T> callback,
                Serialization<T> serialization,
                MessageDeserializer<T> deserializer,
                String name) {
            this.connection = connection;
            this.callback = callback;
            this.serialization = serialization;
            this.deserializer = deserializer;
            this.name = name;
        }

        @Override
        public void write(T message) {
            connection.write(serialization.serialize(message));
        }

        @Override
        public String getName() {
            return name;
        }

        public void onMessage(ByteBuffer buffer) {
            while (buffer.hasRemaining()) {
                if (deserializer.onMessage(buffer)) {
                    send(deserializer.getValue());
                }
            }
        }

        private void send(T message) {
            callback.handleMessageConnection(SerializingConnection.this, message);
        }
    }
}

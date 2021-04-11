package segeraroot.quotesource.infra;

import lombok.AllArgsConstructor;
import segeraroot.connectivity.Connection;
import segeraroot.connectivity.ConnectionCallback;
import segeraroot.connectivity.SimpleConnectionCallback;

import java.nio.ByteBuffer;

@AllArgsConstructor
public class SerializationConnectionCallbackFactory<T> {
    private final MessageSerializer<T> serializer;
    private final MessageDeserializer<T> deserializer;

    public ConnectionCallback<ByteBuffer> handleMessage(ConnectionCallback<T> callback) {
        return SimpleConnectionCallback.<ByteBuffer>builder()
                .newHandler(connection ->
                        callback.handleNewConnection(attach(callback, connection)))
                .closeHandler(connection ->
                        callback.handleCloseConnection(getConnection(connection)))
                .messageHandler(this::handleMessage)
                .build();
    }

    private Connection<T> attach(ConnectionCallback<T> callback, Connection<ByteBuffer> connection) {
        var wrapper = new SerializingConnection<>(
                connection,
                callback,
                serializer,
                deserializer,
                "W|" + connection.getName());
        connection.set(wrapper);
        return wrapper;
    }

    private void handleMessage(Connection<ByteBuffer> connection, ByteBuffer buffer) {
        var wrapper = getConnection(connection);
        wrapper.onMessage(buffer);
    }

    @SuppressWarnings("unchecked")
    private SerializingConnection<T> getConnection(Connection<ByteBuffer> connection) {
        return (SerializingConnection<T>) connection.get();
    }


    private static class SerializingConnection<T>
            extends ConnectionBase<T> {
        private final Connection<ByteBuffer> connection;
        private final ConnectionCallback<T> callback;
        private final MessageSerializer<T> serializer;
        private final MessageDeserializer<T> deserializer;

        private final String name;

        public SerializingConnection(
                Connection<ByteBuffer> connection,
                ConnectionCallback<T> callback,
                MessageSerializer<T> serializer,
                MessageDeserializer<T> deserializer,
                String name) {
            this.connection = connection;
            this.callback = callback;
            this.serializer = serializer;
            this.deserializer = deserializer;
            this.name = name;
        }

        @Override
        public void write(T message) {
            connection.write(serializer.serialize(message));
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

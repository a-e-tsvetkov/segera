package segeraroot.quotesource.infra;

import lombok.AllArgsConstructor;
import segeraroot.connectivity.*;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

@AllArgsConstructor
public class SerializationConnectionCallbackFactory<T, BuilderFactory extends ByteBufferHolder> {
    private final MessageDeserializer<T> deserializer;
    private final Supplier<BuilderFactory> builderFactoryFactory;

    public ConnectionCallback<ByteBuffer, ByteBufferFactory> handleMessage(ConnectionCallback<T, ? super BuilderFactory> callback) {
        return SimpleConnectionCallback.<ByteBuffer, ByteBufferFactory>builder()
                .newHandler(connection ->
                        callback.handleNewConnection(attach(callback, connection)))
                .closeHandler(connection ->
                        callback.handleCloseConnection(getConnection(connection)))
                .messageHandler(this::handleMessage)
                .writingHandler(this::handleWriting)
                .build();
    }

    private Connection<T> attach(ConnectionCallback<T, ? super BuilderFactory> callback, Connection<ByteBuffer> connection) {
        var wrapper = new SerializingConnection<>(
                connection,
                callback,
                deserializer,
                builderFactoryFactory.get(),
                "W|" + connection.getName());
        connection.set(wrapper);
        return wrapper;
    }

    private void handleMessage(Connection<ByteBuffer> connection, ByteBuffer buffer) {
        var wrapper = getConnection(connection);
        wrapper.onMessage(buffer);
    }

    private void handleWriting(Connection<ByteBuffer> connection, ByteBufferFactory byteBufferFactory) {
        var wrapper = getConnection(connection);
        wrapper.write(byteBufferFactory);
    }

    @SuppressWarnings("unchecked")
    private SerializingConnection<T, BuilderFactory> getConnection(Connection<ByteBuffer> connection) {
        return (SerializingConnection<T, BuilderFactory>) connection.get();
    }


    private static class SerializingConnection<T, BuilderFactory extends ByteBufferHolder>
            extends ConnectionBase<T> {
        private final Connection<ByteBuffer> connection;
        private final ConnectionCallback<T, ? super BuilderFactory> callback;
        private final MessageDeserializer<T> deserializer;
        private final BuilderFactory builderFactory;

        private final String name;

        public SerializingConnection(
                Connection<ByteBuffer> connection,
                ConnectionCallback<T, ? super BuilderFactory> callback,
                MessageDeserializer<T> deserializer,
                BuilderFactory builderFactory,
                String name) {
            this.connection = connection;
            this.callback = callback;
            this.deserializer = deserializer;
            this.builderFactory = builderFactory;
            this.name = name;
        }


        public void write(ByteBufferFactory byteBufferFactory) {
            builderFactory.set(byteBufferFactory);
            callback.handleWriting(SerializingConnection.this, builderFactory);
        }

        @Override
        public void startWriting() {
            connection.startWriting();
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
            callback.handleMessage(SerializingConnection.this, message);
        }
    }
}

package segeraroot.quotesource.infra;

import lombok.AllArgsConstructor;
import segeraroot.connectivity.Connection;
import segeraroot.connectivity.ConnectionCallback;
import segeraroot.connectivity.util.ByteBufferFactory;
import segeraroot.connectivity.util.ByteBufferHolder;
import segeraroot.connectivity.util.MessageDeserializer;

import java.nio.ByteBuffer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@AllArgsConstructor
public class SerializationConnectionCallbackFactory<BuilderFactory extends ByteBufferHolder, ReadersVisitor extends ConnectionCallback<BuilderFactory>> {
    private final Function<ReadersVisitor, MessageDeserializer> deserializerFactory;
    private final Supplier<BuilderFactory> builderFactoryFactory;

    public SerializationConnectionCallback handleMessage(ReadersVisitor callback) {
        return new SerializationConnectionCallback(callback);
    }

    @AllArgsConstructor
    public class SerializationConnectionCallback implements ConnectionCallback<ByteBufferFactory>, MessageDeserializer {
        private final ReadersVisitor callback;

        @Override
        public void handleCloseConnection(Connection connection) {
            callback.handleCloseConnection(getConnection(connection));
        }

        @Override
        public void handleNewConnection(Connection connection) {
            callback.handleNewConnection(attach(callback, connection));
        }

        @Override
        public void handleWriting(Connection connection, ByteBufferFactory byteBufferFactory) {
            var wrapper = getConnection(connection);
            wrapper.write(byteBufferFactory);
        }

        @Override
        public void onMessage(Connection connection, ByteBuffer buffer) {
            var wrapper = getConnection(connection);
            wrapper.onMessage(buffer);
        }
    }

    private Connection attach(ReadersVisitor callback, Connection connection) {
        var wrapper = new SerializingConnection<>(
                connection,
                callback,
                deserializerFactory.apply(callback),
                builderFactoryFactory.get(),
                "W|" + connection.getName());
        connection.set(wrapper);
        return wrapper;
    }

    @SuppressWarnings("unchecked")
    private SerializingConnection<Consumer<ReadersVisitor>, BuilderFactory> getConnection(Connection connection) {
        return (SerializingConnection<Consumer<ReadersVisitor>, BuilderFactory>) connection.get();
    }


    private static class SerializingConnection<T, BuilderFactory extends ByteBufferHolder>
            extends ConnectionBase<T> {
        private final Connection connection;
        private final ConnectionCallback<? super BuilderFactory> callback;
        private final MessageDeserializer deserializer;
        private final BuilderFactory builderFactory;

        private final String name;

        public SerializingConnection(
                Connection connection,
                ConnectionCallback<? super BuilderFactory> callback,
                MessageDeserializer deserializer,
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
                deserializer.onMessage(SerializingConnection.this, buffer);
            }
        }
    }
}

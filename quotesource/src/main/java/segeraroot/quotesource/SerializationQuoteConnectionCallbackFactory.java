package segeraroot.quotesource;

import lombok.AllArgsConstructor;
import segeraroot.connectivity.QuoteConnection;
import segeraroot.connectivity.QuoteConnectionCallback;
import segeraroot.connectivity.SimpleQuoteConnectionCallback;
import segeraroot.quotemodel.MessageWrapper;

import java.nio.ByteBuffer;

@AllArgsConstructor
public class SerializationQuoteConnectionCallbackFactory {
    private Serialization serialization;

    public QuoteConnectionCallback<ByteBuffer> handleMessage(QuoteConnectionCallback<MessageWrapper> callback) {
        return SimpleQuoteConnectionCallback.<ByteBuffer>builder()
                .newHandler(connection ->
                        callback.handleNewConnection(attach(callback, connection)))
                .closeHandler(connection ->
                        callback.handleCloseConnection(getConnection(connection)))
                .messageHandler(this::handleMessage)
                .build();
    }

    private QuoteConnection<MessageWrapper> attach(QuoteConnectionCallback<MessageWrapper> callback, QuoteConnection<ByteBuffer> connection) {
        var wrapper = new SerializingConnection(connection, callback, serialization, "W|" + connection.getName());
        connection.set(wrapper);
        return wrapper;
    }

    private void handleMessage(QuoteConnection<ByteBuffer> connection, ByteBuffer buffer) {
        var wrapper = (SerializingConnection) connection.get();
        wrapper.onMessage(buffer);
    }

    private QuoteConnection<MessageWrapper> getConnection(QuoteConnection<ByteBuffer> connection) {
        return (SerializingConnection) connection.get();
    }


    private static class SerializingConnection extends QuoteConnectionBase<MessageWrapper> implements MessageSink<MessageWrapper> {
        private final QuoteConnection<ByteBuffer> connection;
        private final QuoteConnectionCallback<MessageWrapper> callback;
        private final Serialization serialization;
        private final String name;

        public SerializingConnection(QuoteConnection<ByteBuffer> connection, QuoteConnectionCallback<MessageWrapper> callback, Serialization serialization, String name) {
            this.connection = connection;
            this.callback = callback;
            this.serialization = serialization;
            this.name = name;
        }

        @Override
        public void write(MessageWrapper messageWrapper) {
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
        public void send(MessageWrapper message) {
            callback.handleMessageConnection(SerializingConnection.this, message);
        }
    }
}

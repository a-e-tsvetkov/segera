package segeraroot.connectivity.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import segeraroot.connectivity.*;
import segeraroot.connectivity.util.ByteBufferFactory;
import segeraroot.connectivity.util.ByteBufferHolder;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class BaseProtocol<BF extends ByteBufferHolder> implements ProtocolInterface {
    private final ConnectionListener connectionListener;
    private final WriterCallback<BF> writerCallback;
    private final Supplier<BF> builderFactorySupplier;
    private final Supplier<ReaderCallback> messageDeserializeraFactory;

    private final ConnectionListenerImpl connectionListenerImpl = new ConnectionListenerImpl();
    private final ReaderCallbackImplX readerCallbackImpl = new ReaderCallbackImplX();
    private final WriterCallbackImpl writerCallbackImpl = new WriterCallbackImpl();

    @Override
    public ConnectionListener connectionListener() {
        return connectionListenerImpl;
    }

    @Override
    public WriterCallback<ByteBufferFactory> writerCallback() {
        return writerCallbackImpl;
    }

    @Override
    public ReaderCallback readerCallback() {
        return readerCallbackImpl;
    }

    private void handleReading(Connection connection, ByteBuffer buffer) {
        Context<BF> context = connection.get();
        context.readerCallback.onMessage(context.innerConnection, buffer);
    }

    private WritingResult handleWriting(Connection connection, ByteBufferFactory byteBufferFactory) {
        Context<BF> context = connection.get();

        BF builderFactory = context.getBuilderFactory();
        builderFactory.set(byteBufferFactory);
        try {
            return writerCallback.handleWriting(context.innerConnection, builderFactory);
        } finally {
            builderFactory.set(null);
        }
    }

    private void handleCloseConnection(Connection connection) {
        Context<BF> context = connection.get();
        connectionListener.handleCloseConnection(context.innerConnection);
    }

    private Context<BF> handleNewConnection(Connection connection) {
        ContextedConnectionWrapper wrapper = new ContextedConnectionWrapper(connection);
        Object innerContext = connectionListener.handleNewConnection(wrapper);
        wrapper.setContext(innerContext);
        return new Context<>(
                wrapper,
                builderFactorySupplier.get(),
                messageDeserializeraFactory.get());
    }

    @RequiredArgsConstructor
    @Getter
    private static class Context<BF> {
        private final ContextedConnectionWrapper innerConnection;
        private final BF builderFactory;
        private final ReaderCallback readerCallback;
    }

    private class WriterCallbackImpl implements WriterCallback<ByteBufferFactory> {
        @Override
        public WritingResult handleWriting(Connection connection, ByteBufferFactory writer) {
            return BaseProtocol.this.handleWriting(connection, writer);
        }
    }

    private class ReaderCallbackImplX implements ReaderCallback {
        @Override
        public void onMessage(Connection connection, ByteBuffer buffer) {
            BaseProtocol.this.handleReading(connection, buffer);
        }
    }

    private class ConnectionListenerImpl implements ConnectionListener {
        @Override
        public void handleCloseConnection(Connection connection) {
            BaseProtocol.this.handleCloseConnection(connection);
        }

        @Override
        public Context<BF> handleNewConnection(Connection connection) {
            return BaseProtocol.this.handleNewConnection(connection);
        }
    }
}

package segeraroot.quotemodel;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import segeraroot.connectivity.*;
import segeraroot.connectivity.util.ByteBufferFactory;
import segeraroot.quotemodel.impl.BuilderFactoryImpl;
import segeraroot.quotemodel.impl.MessageDeserializerImpl;

import java.nio.ByteBuffer;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ServerProtocol implements ProtocolInterface {
    private final ReadersVisitor readerCallback;
    private final ConnectionListener connectionListener;
    private final WriterCallback<BuilderFactory> writerCallback;
    private final ConnectionListenerImpl connectionListenerImpl = new ConnectionListenerImpl();
    private final ReaderCallbackImplX readerCallbackImpl = new ReaderCallbackImplX();
    private final WriterCallbackImpl writerCallbackImpl = new WriterCallbackImpl();

    public static ProtocolInterface of(
            ReadersVisitor readerCallback,
            ConnectionListener connectionListener,
            WriterCallback<BuilderFactory> writerCallback) {
        return new ServerProtocol(readerCallback, connectionListener, writerCallback);
    }

    private void handleReading(Connection connection, ByteBuffer buffer) {
        Context context = connection.get();
        context.readerCallback.onMessage(context.innerConnection, buffer);
    }

    private WritingResult handleWriting(Connection connection, ByteBufferFactory byteBufferFactory) {
        Context context = connection.get();

        BuilderFactoryImpl builderFactory = context.getBuilderFactory();
        builderFactory.set(byteBufferFactory);
        try {
            return writerCallback.handleWriting(context.innerConnection, builderFactory);
        } finally {
            builderFactory.set(null);
        }
    }

    private void handleCloseConnection(Connection connection) {
        Context context = connection.get();
        connectionListener.handleCloseConnection(context.innerConnection);
    }

    private Context handleNewConnection(Connection connection) {
        ContextedConnectionWrapper wrapper = new ContextedConnectionWrapper(connection);
        Object innerContext = connectionListener.handleNewConnection(wrapper);
        wrapper.setContext(innerContext);
        return new Context(wrapper, new BuilderFactoryImpl(), new MessageDeserializerImpl(readerCallback));
    }

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

    @RequiredArgsConstructor
    @Getter
    public static class Context {
        private final ContextedConnectionWrapper innerConnection;
        private final BuilderFactoryImpl builderFactory;
        private final ReaderCallback readerCallback;
    }

    private class WriterCallbackImpl implements WriterCallback<ByteBufferFactory> {
        @Override
        public WritingResult handleWriting(Connection connection, ByteBufferFactory writer) {
            return ServerProtocol.this.handleWriting(connection, writer);
        }
    }

    private class ReaderCallbackImplX implements ReaderCallback {
        @Override
        public void onMessage(Connection connection, ByteBuffer buffer) {
            ServerProtocol.this.handleReading(connection, buffer);
        }
    }

    private class ConnectionListenerImpl implements ConnectionListener {
        @Override
        public void handleCloseConnection(Connection connection) {
            ServerProtocol.this.handleCloseConnection(connection);
        }

        @Override
        public ServerProtocol.Context handleNewConnection(Connection connection) {
            return ServerProtocol.this.handleNewConnection(connection);
        }
    }
}

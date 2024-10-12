package segeraroot.connectivity.impl;

import lombok.Getter;
import segeraroot.connectivity.Connection;
import segeraroot.connectivity.ProtocolInterface;
import segeraroot.connectivity.callbacks.*;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

public class BaseProtocol<BF extends ByteBufferHolder> implements ProtocolInterface {
    private final WriterCallback<BF> writerCallback;

    private final ConnectionListenerWrapper<Context<BF>> connectionListener;
    private final ReaderCallbackImpl readerCallbackImpl = new ReaderCallbackImpl();
    private final WriterCallbackImpl writerCallbackImpl = new WriterCallbackImpl();

    public BaseProtocol(
            ConnectionListener connectionListener,
            WriterCallback<BF> writerCallback,
            Supplier<BF> builderFactorySupplier,
            Supplier<ReaderCallback> readerCallbackSupplier) {
        this.writerCallback = writerCallback;
        this.connectionListener = new ConnectionListenerWrapper<>(
                connectionListener,
                wrapper -> new Context<>(
                        wrapper,
                        builderFactorySupplier.get(),
                        readerCallbackSupplier.get())
        );
    }

    @Override
    public ConnectionListener connectionListener() {
        return connectionListener;
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
        context.readerCallback.onMessage(context.getInnerConnection(), buffer);
    }

    private OperationResult handleWriting(Connection connection, ByteBufferFactory byteBufferFactory) {
        Context<BF> context = connection.get();

        BF builderFactory = context.getBuilderFactory();
        builderFactory.set(byteBufferFactory);
        try {
            return writerCallback.handleWriting(context.getInnerConnection(), builderFactory);
        } finally {
            builderFactory.set(null);
        }
    }

    @Getter
    private static class Context<BF> extends ContextWrapper {
        private final BF builderFactory;
        private final ReaderCallback readerCallback;

        public Context(Connection innerConnection, BF builderFactory, ReaderCallback readerCallback) {
            super(innerConnection);
            this.builderFactory = builderFactory;
            this.readerCallback = readerCallback;
        }
    }

    private class WriterCallbackImpl implements WriterCallback<ByteBufferFactory> {
        @Override
        public OperationResult handleWriting(Connection connection, ByteBufferFactory writer) {
            return BaseProtocol.this.handleWriting(connection, writer);
        }
    }

    private class ReaderCallbackImpl implements ReaderCallback {
        @Override
        public void onMessage(Connection connection, ByteBuffer buffer) {
            BaseProtocol.this.handleReading(connection, buffer);
        }
    }
}

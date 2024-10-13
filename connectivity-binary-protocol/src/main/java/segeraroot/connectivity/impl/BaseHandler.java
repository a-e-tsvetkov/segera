package segeraroot.connectivity.impl;

import lombok.experimental.Delegate;
import segeraroot.connectivity.Connection;
import segeraroot.connectivity.ProtocolInterface;
import segeraroot.connectivity.callbacks.*;

import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class BaseHandler implements ConnectivityHandler {
    private final ProtocolInterface protocol;

    private final ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
    private final ByteBufferFactoryImpl byteBufferFactory = new ByteBufferFactoryImpl();
    @Delegate
    private final ConnectionListenerWrapper<Context> connectionListener;

    protected BaseHandler(ProtocolInterface protocol) {
        this.protocol = protocol;
        connectionListener = new ConnectionListenerWrapper<>(
            protocol.connectionListener(),
            Context::new
        );

    }

    @Override
    public final void read(ConnectivityChanel channel, Connection connection) throws IOException {
        Context context = connection.get();
        buffer.position(0);
        buffer.limit(buffer.capacity());
        channel.read(buffer);
        buffer.flip();
        doRead(buffer, channel, context);
    }

    protected void doRead(ByteBuffer buffer, ConnectivityChanel channel, Context context) {
        while (buffer.hasRemaining()) {
            protocol.readerCallback().onMessage(context.getInnerConnection(), buffer);
        }
    }

    @Override
    public final OperationResult write(ConnectivityChanel chanel, Connection connection) throws IOException {
        Context context = connection.get();
        return doWrite(buffer, chanel, context);
    }

    protected OperationResult doWrite(ByteBuffer buffer, ConnectivityChanel chanel, Context context) throws IOException {
        buffer.position(0);
        buffer.limit(buffer.capacity());
        OperationResult result = protocol.writerCallback()
            .handleWriting(context.getInnerConnection(), byteBufferFactory);
        buffer.flip();
        chanel.write(buffer);
        return result;
    }

    private class ByteBufferFactoryImpl implements ByteBufferFactory {

        @Override
        public boolean write(WriteCallback callback) {
            return callback.tryWrite(buffer);
        }
    }

    protected static class Context extends ContextWrapper {
        public Context(Connection wrapper) {
            super(wrapper);
        }
    }
}

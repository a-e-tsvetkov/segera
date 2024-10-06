package segeraroot.connectivity.http.impl;

import lombok.Getter;
import lombok.experimental.Delegate;
import segeraroot.connectivity.Connection;
import segeraroot.connectivity.callbacks.ConnectionListener;
import segeraroot.connectivity.callbacks.ConnectivityChanel;
import segeraroot.connectivity.callbacks.ConnectivityHandler;
import segeraroot.connectivity.callbacks.OperationResult;
import segeraroot.connectivity.http.EndpointCallback;
import segeraroot.connectivity.impl.ConnectionListenerWrapper;
import segeraroot.connectivity.impl.ContextWrapper;

import java.io.IOException;
import java.nio.ByteBuffer;

public class HttpServerHandler implements ConnectivityHandler {
    public static final int CAPACITY = 1024;

    private final ByteStream byteStream;
    @Delegate(types = ConnectionListener.class)
    private final ConnectionListenerWrapper<Context> connectionListener;

    public HttpServerHandler(EndpointCallback endpointCallback) {
        byteStream = new ByteStream(CAPACITY);
        connectionListener = new ConnectionListenerWrapper<>(
                ConnectionListener.NOOP,
                wrapper -> new Context(
                        wrapper,
                        new HttpDecoder(endpointCallback))
        );
    }

    @Override
    public final void read(ConnectivityChanel channel, Connection connection) throws IOException {
        Context context = connectionListener.unwrap(connection);
        byteStream.readFrom(channel);
        doRead(byteStream, context);
    }

    private void doRead(ByteStream byteStream, Context context) {
        OperationResult requestHandler = context.getHttpDecoder().onMessage(byteStream);
        if (requestHandler == OperationResult.DONE) {
            context.getInnerConnection().startWriting();
        }
    }

    @Override
    public final OperationResult write(ConnectivityChanel chanel, Connection connection) throws IOException {
        Context context = connectionListener.unwrap(connection);
        ByteBuffer buffer = byteStream.byteBuffer();
        buffer.position(0);
        buffer.limit(buffer.capacity());
        OperationResult result = writeResponse(context, buffer);
        buffer.flip();
        chanel.write(buffer);
        assert !buffer.hasRemaining();
        return result;
    }

    private OperationResult writeResponse(Context context, ByteBuffer buffer) {
        return context.getHttpDecoder().onWrite(buffer);
    }

    @Getter
    private static class Context extends ContextWrapper {
        private final HttpDecoder httpDecoder;
        public Context(Connection wrapper, HttpDecoder decoder) {
            super(wrapper);
            this.httpDecoder = decoder;
        }
    }
}

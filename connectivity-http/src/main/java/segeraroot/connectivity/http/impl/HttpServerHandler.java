package segeraroot.connectivity.http.impl;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import segeraroot.connectivity.Connection;
import segeraroot.connectivity.callbacks.*;
import segeraroot.connectivity.http.EndpointCallback;
import segeraroot.connectivity.impl.ConnectionListenerWrapper;
import segeraroot.connectivity.impl.ContextWrapper;
import segeraroot.connectivity.impl.ContextedConnectionWrapper;

import java.io.IOException;
import java.nio.ByteBuffer;

public class HttpServerHandler implements ConnectivityHandler {
    public static final int CAPACITY = 1024;

    private final ByteBuffer buffer;
    @Delegate(types = ConnectionListener.class)
    private final ConnectionListenerWrapper<Context> connectionListener;

    public HttpServerHandler(EndpointCallback endpointCallback) {
        buffer = ByteBuffer.allocateDirect(CAPACITY);
        connectionListener = new ConnectionListenerWrapper<>(
                ConnectionListener.NOOP,
                wrapper -> new Context(
                        wrapper,
                        new ByteStream(CAPACITY),
                        new HttpDecoder(endpointCallback))
        );
    }

    @Override
    public final void read(ConnectivityChanel channel, Connection connection) throws IOException {
        Context context = connectionListener.unwrap(connection);
        buffer.position(0);
        buffer.limit(buffer.capacity());
        channel.read(buffer);
        buffer.flip();
        doRead(buffer, context);
    }

    private void doRead(ByteBuffer buffer, Context context) {
        var byteStream = context.getByteStream();
        byteStream.copy(buffer);
        while (byteStream.hasRemaining()) {
            RequestHandler requestHandler = context.getHttpDecoder().onMessage(byteStream);
            if (requestHandler != null) {
                context.setRequestHandler(requestHandler);
                context.getInnerConnection().startWriting();
            }
        }
    }

    @Override
    public final WritingResult write(ConnectivityChanel chanel, Connection connection) throws IOException {
        Context context = connectionListener.unwrap(connection);
        return doWrite(buffer, chanel, context);
    }

    private WritingResult doWrite(ByteBuffer buffer, ConnectivityChanel chanel, Context context) throws IOException {
        buffer.position(0);
        buffer.limit(buffer.capacity());
        WritingResult result = writeResponse(context, buffer);
        buffer.flip();
        chanel.write(buffer);
        return result;
    }

    private WritingResult writeResponse(Context context, ByteBuffer buffer) {
        return context.getRequestHandler().onWrite(buffer);
    }

    @Getter
    private static class Context extends ContextWrapper {
        private final ByteStream byteStream;
        private final HttpDecoder httpDecoder;
        @Setter
        private volatile RequestHandler requestHandler;

        public Context(ContextedConnectionWrapper wrapper, ByteStream byteStream, HttpDecoder decoder) {
            super(wrapper);
            this.byteStream = byteStream;
            this.httpDecoder = decoder;
        }
    }
}

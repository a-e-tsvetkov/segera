package segeraroot.connectivity.http.impl;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import segeraroot.connectivity.Connection;
import segeraroot.connectivity.callbacks.ConnectionListener;
import segeraroot.connectivity.callbacks.ConnectivityChanel;
import segeraroot.connectivity.callbacks.ConnectivityHandler;
import segeraroot.connectivity.callbacks.WritingResult;
import segeraroot.connectivity.http.EndpointCallback;
import segeraroot.connectivity.impl.ConnectionListenerWrapper;
import segeraroot.connectivity.impl.ContextWrapper;

import java.io.IOException;
import java.nio.ByteBuffer;

public class HttpServerHandler implements ConnectivityHandler {
    public static final int CAPACITY = 4*1024;

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
        return doWrite(byteStream.byteBuffer(), chanel, context);
    }

    private WritingResult doWrite(ByteBuffer buffer, ConnectivityChanel chanel, Context context) throws IOException {
        buffer.position(0);
        buffer.limit(buffer.capacity());
        WritingResult result = writeResponse(context, buffer);
        buffer.flip();
        chanel.write(buffer);
        assert !buffer.hasRemaining();
        return result;
    }

    private WritingResult writeResponse(Context context, ByteBuffer buffer) {
        return context.getRequestHandler().onWrite(buffer);
    }

    @Getter
    private static class Context extends ContextWrapper {
        private final HttpDecoder httpDecoder;
        @Setter
        private volatile RequestHandler requestHandler;

        public Context(Connection wrapper, HttpDecoder decoder) {
            super(wrapper);
            this.httpDecoder = decoder;
        }
    }
}

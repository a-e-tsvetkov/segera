package segeraroot.quotemodel;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import segeraroot.connectivity.*;
import segeraroot.connectivity.callbacks.ConnectionListener;
import segeraroot.connectivity.callbacks.WriterCallback;
import segeraroot.connectivity.impl.ClientHandler;
import segeraroot.connectivity.impl.ServerHandler;
import segeraroot.quotemodel.impl.BuilderFactoryImpl;
import segeraroot.quotemodel.impl.MessageDeserializerImpl;

public class Protocol {

    public static ServerBuilder server(
        ReadersVisitor readerCallback,
        ConnectionListener connectionListener,
        WriterCallback<BuilderFactory> writerCallback
    ) {
        return new ServerBuilder(readerCallback, connectionListener, writerCallback);
    }

    public static ClientBuilder client(
        ReadersVisitor readerCallback,
        ConnectionListener connectionListener,
        WriterCallback<BuilderFactory> writerCallback
    ) {
        return new ClientBuilder(readerCallback, connectionListener, writerCallback);
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ServerBuilder {
        private final ReadersVisitor readerCallback;
        private final ConnectionListener connectionListener;
        private final WriterCallback<BuilderFactory> writerCallback;

        public Server at(int port) {
            var protocol = ServerProtocol.of(
                connectionListener,
                adapt(writerCallback),
                BuilderFactoryImpl::new,
                () -> new MessageDeserializerImpl(readerCallback));
            return new Server(port, new ServerHandler(protocol));
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ClientBuilder {
        private final ReadersVisitor readerCallback;
        private final ConnectionListener connectionListener;
        private final WriterCallback<BuilderFactory> writerCallback;

        public Client connectTo(String host, int port) {
            var clientProtocol = ClientProtocol.of(
                connectionListener,
                adapt(writerCallback),
                BuilderFactoryImpl::new,
                () -> new MessageDeserializerImpl(readerCallback));
            return new Client(host, port, new ClientHandler(clientProtocol));
        }
    }

    // There is only one implementation of BuilderFactory
    @SuppressWarnings("unchecked")
    private static WriterCallback<BuilderFactoryImpl> adapt(WriterCallback<BuilderFactory> writerCallback) {
        return (WriterCallback<BuilderFactoryImpl>) (Object) writerCallback;
    }
}

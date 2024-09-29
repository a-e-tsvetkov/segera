package segeraroot.connectivity;

import lombok.extern.slf4j.Slf4j;
import segeraroot.connectivity.impl.Connector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;


@Slf4j
public class Server extends Connector {
    private final int port;

    public Server(int port, ProtocolInterface protocol) {
        super(protocol);
        this.port = port;
    }

    @Override
    protected void onStart(Selector selector) throws IOException {
        super.onStart(selector);
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        log.info("Start server: port={}", port);
        serverSocketChannel.bind(new InetSocketAddress(port));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    @Override
    protected void onHeaderWrite(ByteBuffer buffer) {
        doWriteHeader(buffer);
    }
}

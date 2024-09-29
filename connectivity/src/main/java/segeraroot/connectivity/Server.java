package segeraroot.connectivity;

import lombok.extern.slf4j.Slf4j;
import segeraroot.connectivity.callbacks.ConnectivityHandler;
import segeraroot.connectivity.impl.Connector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;


@Slf4j
public class Server extends Connector {
    private final int port;

    public Server(int port, ConnectivityHandler handler) {
        super(handler);
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

}

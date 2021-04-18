package segeraroot.connectivity.impl;

import lombok.extern.slf4j.Slf4j;
import segeraroot.connectivity.Connection;
import segeraroot.connectivity.ConnectionCallback;
import segeraroot.connectivity.util.ByteBufferFactory;
import segeraroot.connectivity.util.MessageDeserializer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@Slf4j
public class Client<T extends ConnectionCallback<ByteBufferFactory> & MessageDeserializer>
        extends Connector<T> {
    private final String host;
    private final int port;
    //TODO: Switch to zero memory footprint solution
    private final ScheduledExecutorService scheduledExecutorService;
    private final Runnable reconnectRunnable;

    public Client(String host, int port, T connectionCallback) {
        super(connectionCallback);
        this.host = host;
        this.port = port;

        scheduledExecutorService = Executors.newScheduledThreadPool(1);

        reconnectRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    Client.this.doStart();
                } catch (IOException e) {
                    log.error("Unable to connect: {}", e.getMessage());
                    scheduledExecutorService.schedule(this, 10, TimeUnit.SECONDS);
                }
            }
        };
    }

    @Override
    protected void onStart(Selector selector) throws IOException {
        super.onStart(selector);
        doStart();
    }

    private void doStart() throws IOException {
        SocketChannel channel = SocketChannel.open();
        log.info("Start client: port={}", port);
        channel.connect(new InetSocketAddress(host, port));
        connectionEstablished(channel);
    }

    @Override
    protected void onClose(Connection handler) {
        super.onClose(handler);
        if (!isRunning) {
            scheduledExecutorService.schedule(reconnectRunnable, 10, TimeUnit.SECONDS);
        }
    }

    @Override
    protected void onHeaderRead(ByteBuffer buffer) {
        readHeader(buffer);
    }
}

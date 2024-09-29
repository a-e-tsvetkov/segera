package segeraroot.connectivity.impl;

import lombok.extern.slf4j.Slf4j;
import segeraroot.connectivity.ProtocolDescriptor;
import segeraroot.connectivity.ProtocolInterface;
import segeraroot.connectivity.callbacks.ConnectivityChanel;

import java.nio.ByteBuffer;


@Slf4j
public class ClientHandler extends BaseHandler {
    private volatile boolean isHeaderProcessed = false;

    public ClientHandler(ProtocolInterface protocol) {
        super(protocol);
    }

 @Override
 protected void doRead(ByteBuffer buffer, ConnectivityChanel channel, Context context) {
        if (!isHeaderProcessed) {
            readHeader(buffer, channel);
            isHeaderProcessed = true;
        }
        super.doRead(buffer, channel, context);
    }

    protected final void readHeader(ByteBuffer buffer, ConnectivityChanel channel) {
        int version = buffer.getInt();
        if (version != ProtocolDescriptor.VERSION) {
            log.error("Wrong version: expected={} actual={}", ProtocolDescriptor.VERSION, version);
            channel.stop();
        }
    }
}

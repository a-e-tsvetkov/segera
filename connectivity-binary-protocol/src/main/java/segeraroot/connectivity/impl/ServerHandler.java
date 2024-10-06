package segeraroot.connectivity.impl;

import lombok.extern.slf4j.Slf4j;
import segeraroot.connectivity.ProtocolDescriptor;
import segeraroot.connectivity.ProtocolInterface;
import segeraroot.connectivity.callbacks.ConnectivityChanel;
import segeraroot.connectivity.callbacks.OperationResult;

import java.io.IOException;
import java.nio.ByteBuffer;

@Slf4j
public class ServerHandler extends BaseHandler {
    private volatile boolean headerWrite = false;

    public ServerHandler(ProtocolInterface protocol) {
        super(protocol);
    }

    @Override
    protected OperationResult doWrite(ByteBuffer buffer, ConnectivityChanel chanel, Context context) throws IOException {
        if (!headerWrite) {
            doWriteHeader(buffer, chanel, context);
            headerWrite = true;
        }
        return super.doWrite(buffer, chanel, context);
    }

    private void doWriteHeader(ByteBuffer buffer, ConnectivityChanel chanel, Context context) throws IOException {
        log.trace("doWrite: Writing header: {}", context.getInnerConnection().getName());
        buffer.position(0);
        buffer.limit(buffer.capacity());

        buffer.putInt(ProtocolDescriptor.VERSION);
        buffer.flip();
        chanel.write(buffer);
        assert buffer.remaining() == 0;
    }
}

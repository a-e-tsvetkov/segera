package segeraroot.connectivity;

import segeraroot.connectivity.util.ByteBufferFactory;

public interface ProtocolInterface {
    ConnectionListener connectionListener();

    WriterCallback<ByteBufferFactory> writerCallback();

    ReaderCallback readerCallback();
}

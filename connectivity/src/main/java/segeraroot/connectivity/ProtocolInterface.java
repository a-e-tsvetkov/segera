package segeraroot.connectivity;

import segeraroot.connectivity.callbacks.ByteBufferFactory;
import segeraroot.connectivity.callbacks.ConnectionListener;
import segeraroot.connectivity.callbacks.ReaderCallback;
import segeraroot.connectivity.callbacks.WriterCallback;

public interface ProtocolInterface {
    ConnectionListener connectionListener();

    WriterCallback<ByteBufferFactory> writerCallback();

    ReaderCallback readerCallback();
}

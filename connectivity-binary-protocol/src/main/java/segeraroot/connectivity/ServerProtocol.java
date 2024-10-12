package segeraroot.connectivity;

import lombok.experimental.UtilityClass;
import segeraroot.connectivity.callbacks.ConnectionListener;
import segeraroot.connectivity.callbacks.ReaderCallback;
import segeraroot.connectivity.callbacks.WriterCallback;
import segeraroot.connectivity.impl.BaseProtocol;
import segeraroot.connectivity.impl.ByteBufferHolder;

import java.util.function.Supplier;

@UtilityClass
public class ServerProtocol {
    public static <BF extends ByteBufferHolder> ProtocolInterface of(
            ConnectionListener connectionListener,
            WriterCallback<BF> writerCallback,
            Supplier<BF> builderFactorySupplier,
            Supplier<ReaderCallback> readerCallbackSupplier) {
        return new BaseProtocol<>(
                connectionListener,
                writerCallback,
                builderFactorySupplier,
                readerCallbackSupplier
        );
    }
}

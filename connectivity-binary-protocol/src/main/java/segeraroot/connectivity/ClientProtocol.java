package segeraroot.connectivity;

import lombok.experimental.UtilityClass;
import segeraroot.connectivity.impl.BaseProtocol;
import segeraroot.connectivity.util.ByteBufferHolder;

import java.util.function.Supplier;

@UtilityClass
public class ClientProtocol {
    public static <BF extends ByteBufferHolder> ProtocolInterface of(
            ConnectionListener connectionListener,
            WriterCallback<BF> writerCallback,
            Supplier<BF> builderFactorySupplier,
            Supplier<ReaderCallback> messageDeserializeraFactory) {
        return new BaseProtocol<>(
                connectionListener,
                writerCallback,
                builderFactorySupplier,
                messageDeserializeraFactory
        );
    }
}

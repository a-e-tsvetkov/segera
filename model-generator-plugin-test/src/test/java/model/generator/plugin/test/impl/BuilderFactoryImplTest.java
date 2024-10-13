package model.generator.plugin.test.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import segeraroot.connectivity.callbacks.ByteBufferFactory;
import segeraroot.connectivity.callbacks.WriteCallback;

import java.nio.ByteBuffer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuilderFactoryImplTest {

    @InjectMocks
    BuilderFactoryImpl builderFactory;
    @Mock
    ByteBufferFactory byteBuilderFactory;

    @Test
    void createTestMessage() {
        byte[] testArray = {1, 2, 3, 4, 5, 6};
        ByteBuffer buffer = ByteBuffer.allocate(19);
        builderFactory.set(byteBuilderFactory);
        when(byteBuilderFactory.write(Mockito.any())).thenAnswer(args -> {
            WriteCallback callback = args.getArgument(0);
            return write(callback, buffer);
        });
        boolean result = builderFactory.createTestMessage()
            .field1(0x7777777777777777L)
            .field2(2)
            .field3(testArray)
            .send();

        assertThat(buffer.array()).asHexString()
            .isEqualTo(
                "00" +
                    "7777777777777777" +
                    "00000002" +
                    "010203040506"
            );

        assertThat(result).isTrue();
    }

    public boolean write(WriteCallback consumer, ByteBuffer buffer) {
        buffer.position(0);
        buffer.limit(buffer.capacity());
        boolean success = consumer.tryWrite(buffer);
        if (!success) {
            return false;
        }
        buffer.flip();
        return true;
    }
}

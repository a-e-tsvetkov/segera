package model.generator.plugin.test.impl;

import model.generator.plugin.test.ReadersVisitor;
import model.generator.plugin.test.writers.TestMessageReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import segeraroot.connectivity.Connection;

import java.nio.ByteBuffer;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageDeserializerImplTest {
    @InjectMocks
    MessageDeserializerImpl deserializer;
    @Mock
    ReadersVisitor<?> visitor;
    @Mock
    Connection connection;

    @Test
    void parseBody() {
        byte[] testArray = {1, 2, 3, 4, 5, 6};
        ByteBuffer buffer = ByteBuffer.allocate(19);

        buffer.put(HexFormat.of().parseHex("00"));
        buffer.put(HexFormat.of().parseHex("7777777777777777"));
        buffer.put(HexFormat.of().parseHex("00000002"));
        buffer.put(testArray);
        buffer.flip();

        doAnswer(arguments -> {
            TestMessageReader reader = arguments.getArgument(1);
            assertThat(reader.field1()).isEqualTo(0x7777777777777777L);
            assertThat(reader.field2()).isEqualTo(2);
            assertThat(reader.field3()).isEqualTo(testArray);
            return null;
        }).when(visitor).visit(Mockito.any(), Mockito.any());

        while (buffer.hasRemaining()) {
            deserializer.onMessage(connection, buffer);
        }
        verify(visitor).visit(Mockito.any(), Mockito.any());

    }
}

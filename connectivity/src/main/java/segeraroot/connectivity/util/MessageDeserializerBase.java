package segeraroot.connectivity.util;

import segeraroot.connectivity.Connection;

import java.nio.ByteBuffer;

public abstract class MessageDeserializerBase<ReadersVisitor, MessageType> implements MessageDeserializer {
    protected final ReadersVisitor callback;
    protected ReadingState readingState = ReadingState.START;
    protected MessageType messageType;

    public MessageDeserializerBase(ReadersVisitor callback) {
        this.callback = callback;
    }

    public void onMessage(Connection connection, ByteBuffer buffer) {
        switch (readingState) {
            case START:
                MessageType messageType = toType(buffer.get());
                readingState = ReadingState.IN_MESSAGE;
                this.messageType = messageType;
                break;
            case IN_MESSAGE:
                parseBody(connection, buffer);
                break;
        }
    }

    protected enum ReadingState {
        START,
        IN_MESSAGE
    }

    protected abstract void parseBody(Connection connection, ByteBuffer buffer);

    protected abstract MessageType toType(byte b);
}

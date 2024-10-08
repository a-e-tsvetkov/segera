package segeraroot.connectivity.impl;

import segeraroot.connectivity.Connection;
import segeraroot.connectivity.callbacks.ReaderCallback;

import java.nio.ByteBuffer;

public abstract class ReaderCallbackBase<ReadersVisitor> implements ReaderCallback {
    protected final ReadersVisitor callback;
    protected ReadingState readingState = ReadingState.START;
    protected byte messageType;

    public ReaderCallbackBase(ReadersVisitor callback) {
        this.callback = callback;
    }

    public void onMessage(Connection connection, ByteBuffer buffer) {
        switch (readingState) {
            case START:
                byte messageType = buffer.get();
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
}

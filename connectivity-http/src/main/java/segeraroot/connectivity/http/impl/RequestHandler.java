package segeraroot.connectivity.http.impl;

import segeraroot.connectivity.callbacks.OperationResult;

import java.nio.ByteBuffer;

public interface RequestHandler {

    OperationResult onMessage(ByteStream byteStream);

    OperationResult onWrite(ByteBuffer buffer);
}

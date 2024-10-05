package segeraroot.connectivity.http.impl;

import segeraroot.connectivity.callbacks.WritingResult;
import segeraroot.connectivity.http.HttpMethod;

import java.nio.ByteBuffer;
import java.util.Map;

public interface RequestHandler {
    boolean onHeader(HttpMethod method, String path, Map<String, String> header);

    boolean onMessage(ByteStream byteStream);

    WritingResult onWrite(ByteBuffer buffer);
}

package segeraroot.connectivity.http;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import segeraroot.connectivity.callbacks.OperationResult;
import segeraroot.connectivity.http.impl.*;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;

@Slf4j
@RequiredArgsConstructor
public class StaticPageEndpointCallback implements RequestHandler {
    private final String content;
    private final LineWriter current = new LineWriter();
    private final HeaderDecoder headerDecoder = new HeaderDecoder();

    private volatile State state = State.START;
    private volatile Deque<String> headers;

    private enum State {
        START,
        READ_HEADER,
        READ_BODY,
        WRITING_START,
        WRITE_HEADER,
        WRITING_STATUS,
        WRITE_BODY,
        WRITE_BODY_SEPARATOR, DONE
    }

    private String formatHeader(String name, String value) {
        return "%s: %s\r\n".formatted(name, value);
    }

    @Override
    public OperationResult onMessage(ByteStream byteStream) {
        switch (state) {
            case START:
                state = State.READ_HEADER;
            case READ_HEADER:
                while (byteStream.hasRemaining()) {
                    if (headerDecoder.onMessage(byteStream) == OperationResult.CONTINUE) {
                        return OperationResult.CONTINUE;
                    }
                    if (headerDecoder.isDone()) {
                        state = State.READ_BODY;
                        break;
                    }
                    log.trace("onMessage: header {}={}", headerDecoder.getKey(), headerDecoder.getValue());
                    onHeader(headerDecoder.getKey(), headerDecoder.getValue());
                }
            case READ_BODY:
                if (readBody() == OperationResult.DONE) {
                    return OperationResult.CONTINUE;
                }
                state = State.WRITING_START;
        }
        return OperationResult.DONE;
    }

    private OperationResult readBody() {
        return null;
    }

    private void onHeader(String key, String value) {

    }

    @Override
    public OperationResult onWrite(ByteBuffer buffer) {
        log.trace("onWrite: state = {}", state);
        switch (state) {
            case WRITING_START:
                current.set(formatStatusLine());
                state = State.WRITING_STATUS;
                log.trace("onWrite: finish {}", state);
            case WRITING_STATUS:
                if (current.write(buffer) == OperationResult.CONTINUE) {
                    return OperationResult.CONTINUE;
                }
                state = State.WRITE_HEADER;
                headers = writeHeaders();
                // TODO: This is a hack. Fix it.
                current.set("");
                log.trace("onWrite: finish {}", state);
            case WRITE_HEADER:
                while (true) {
                    if (current.write(buffer) == OperationResult.CONTINUE) {
                        return OperationResult.CONTINUE;
                    }
                    String next = headers.poll();
                    if (next == null) {
                        break;
                    }
                    current.set(next);
                }
                current.set("\r\n");
                state = State.WRITE_BODY_SEPARATOR;
                log.trace("onWrite: finish {}", state);
            case WRITE_BODY_SEPARATOR:
                if (current.write(buffer) == OperationResult.CONTINUE) {
                    return OperationResult.CONTINUE;
                }
                current.set(content);
                state = State.WRITE_BODY;
                log.trace("onWrite: finish {}", state);
            case WRITE_BODY:
                if (current.write(buffer) == OperationResult.CONTINUE) {
                    return OperationResult.CONTINUE;
                }
                state = State.DONE;
                log.trace("onWrite: finish {}", state);
        }
        return OperationResult.DONE;
    }

    private Deque<String> writeHeaders() {
        Deque<String> headers = new ArrayDeque<>();
        headers.add(formatHeader(Headers.ContentType, "text/html; charset=US_ASCII"));
        headers.add(formatHeader(Headers.ContentLength, String.valueOf(content.length())));
//        headers.add(formatHeader(Headers.Connection, "keep-alive"));
        headers.add(formatHeader(Headers.Connection, "close"));
        return headers;
    }

    private String formatStatusLine() {
        return "HTTP/1.1 200 OK\r\n";
    }
}

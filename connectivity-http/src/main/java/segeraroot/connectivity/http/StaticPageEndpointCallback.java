package segeraroot.connectivity.http;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import segeraroot.connectivity.callbacks.WritingResult;
import segeraroot.connectivity.http.impl.ByteStream;
import segeraroot.connectivity.http.impl.LineWriter;
import segeraroot.connectivity.http.impl.RequestHandler;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;

@Slf4j
@RequiredArgsConstructor
public class StaticPageEndpointCallback implements RequestHandler {
    private final String content;
    private final LineWriter current = new LineWriter();
    private final Queue<String> headers = new ArrayDeque<>();

    private enum State {START, HEADER, BODY, STATUS}

    private volatile State state = State.START;

    @Override
    public boolean onHeader(HttpMethod method, String path, Map<String, String> header) {
        headers.add(formatHeader(Headers.ContentType, "text/html; charset=US_ASCII"));
        headers.add(formatHeader(Headers.ContentLength, String.valueOf(content.length())));
        headers.add("\r\n");
        return true;
    }

    private String formatHeader(String name, String value) {
        return "%s: %s\r\n".formatted(name, value);
    }

    @Override
    public boolean onMessage(ByteStream byteStream) {
        return false;
    }

    @Override
    public WritingResult onWrite(ByteBuffer buffer) {
        log.trace("onWrite: state = {}", state);
        switch (state) {
            case START:
                current.set(formatStatusLine());
            case STATUS:
                if (current.write(buffer)) {
                    current.set(headers.poll());
                    state = State.HEADER;
                }
                break;
            case HEADER:
                if (current.write(buffer)) {
                    String next = headers.poll();
                    if (next == null) {
                        current.set(content);
                        state = State.BODY;
                    } else {
                        current.set(next);
                    }
                }
                break;
            case BODY:
                if (current.write(buffer)) {
                    return WritingResult.DONE;
                }

        }
        return WritingResult.CONTINUE;
    }

    private String formatStatusLine() {
        return "HTTP/1.1 200 OK\r\n";
    }
}

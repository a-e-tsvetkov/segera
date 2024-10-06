package segeraroot.connectivity.http.impl;

import lombok.extern.slf4j.Slf4j;
import segeraroot.connectivity.http.EndpointCallback;

@Slf4j
public class HttpDecoder {
    private final RequestLineDecoder requestLineDecoder = new RequestLineDecoder();
    private final HeaderDecoder headerParser = new HeaderDecoder();
    private final EndpointCallback endpointCallback;
    private volatile RequestHandler requestHandler;

    public HttpDecoder(EndpointCallback endpointCallback) {
        this.endpointCallback = endpointCallback;
    }

    enum State {
        HEADER, ATTRIBUTES, BODY, DONE
    }

    private volatile State state = State.HEADER;

    public RequestHandler onMessage(ByteStream byteStream) {
        switch (state) {
            case HEADER -> {
                var result = requestLineDecoder.onMessage(byteStream);
                if (result) {
                    log.trace("onMessage: request line done: method = {} url = {}",
                            requestLineDecoder.getMethod(),
                            requestLineDecoder.getPath());
                    state = State.ATTRIBUTES;
                }
            }
            case ATTRIBUTES -> {
                var result = headerParser.onMessage(byteStream);
                if (result) {
                    if (log.isTraceEnabled()) {
                        log.trace("onMessage: header done");
                        headerParser.getHeader().forEach((key, value) ->
                                log.trace("\t{} = {}", key, value));
                    }
                    state = State.BODY;
                    requestHandler = endpointCallback.route(requestLineDecoder.getMethod(), requestLineDecoder.getPath());
                    if (requestHandler.onHeader(
                            requestLineDecoder.getMethod(),
                            requestLineDecoder.getPath(),
                            headerParser.getHeader())) {
                        return requestHandler;
                    }
                }
            }
            case BODY -> {
                var result = requestHandler.onMessage(byteStream);
                if (result) {
                    state = State.DONE;
                    return requestHandler;
                }
            }
        }
        return null;
    }
}

package segeraroot.connectivity.http.impl;

import lombok.extern.slf4j.Slf4j;
import segeraroot.connectivity.callbacks.OperationResult;
import segeraroot.connectivity.http.EndpointCallback;

import java.nio.ByteBuffer;

@Slf4j
public class HttpDecoder {
    private final RequestLineDecoder requestLineDecoder = new RequestLineDecoder();
    private final EndpointCallback endpointCallback;

    private volatile State state = State.HEADER;
    private volatile RequestHandler requestHandler;

    public HttpDecoder(EndpointCallback endpointCallback) {
        this.endpointCallback = endpointCallback;
    }

    enum State {
        HEADER, PROESSING
    }

    public OperationResult onMessage(ByteStream byteStream) {
        switch (state) {
            case HEADER:
                if (requestLineDecoder.onMessage(byteStream) == OperationResult.CONTINUE) {
                    return OperationResult.CONTINUE;
                }
                log.trace("onMessage: request line done: method = {} url = {}",
                        requestLineDecoder.getMethod(),
                        requestLineDecoder.getPath());
                state = State.PROESSING;
                requestHandler = endpointCallback.route(requestLineDecoder.getMethod(), requestLineDecoder.getPath());
                assert requestHandler != null;
            case PROESSING:
                if (requestHandler.onMessage(byteStream) == OperationResult.CONTINUE) {
                    return OperationResult.CONTINUE;
                }
        }
        return OperationResult.DONE;
    }

    public OperationResult onWrite(ByteBuffer buffer) {
        OperationResult result = requestHandler.onWrite(buffer);
        if (result == OperationResult.DONE){
            reset();
        }
        return result;
    }

    private void reset() {
        requestHandler = null;
        state = State.HEADER;
    }
}

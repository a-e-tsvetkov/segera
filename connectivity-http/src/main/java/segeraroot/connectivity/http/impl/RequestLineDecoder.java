package segeraroot.connectivity.http.impl;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import segeraroot.connectivity.callbacks.OperationResult;
import segeraroot.connectivity.http.HttpMethod;
import segeraroot.connectivity.http.HttpPath;

@Slf4j
public class RequestLineDecoder {

    private final StringBuilder builder = new StringBuilder();
    @Getter
    private HttpMethod method;
    @Getter
    private HttpPath path;

    public OperationResult onMessage(ByteStream byteStream) {
        OperationResult result = byteStream.readLine(builder);
        if (result == OperationResult.CONTINUE) {
            return OperationResult.CONTINUE;
        }
        String string = builder.toString();
        log.trace("onMessage: {}", string);
        String[] parts = string.split(" ");
        method = HttpMethod.valueOf(parts[0]);
        //TODO: Fix that hack. Need proper parsing
        path = HttpPath.parse(parts[1]).rest(0);
        builder.setLength(0);
        return OperationResult.DONE;
    }
}

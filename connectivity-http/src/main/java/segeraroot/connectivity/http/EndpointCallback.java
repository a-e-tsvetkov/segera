package segeraroot.connectivity.http;

import lombok.RequiredArgsConstructor;
import segeraroot.connectivity.http.impl.RequestHandler;

@RequiredArgsConstructor
public class EndpointCallback {
    private final RequestHandler defaultHandler;

    public RequestHandler route(HttpMethod method, String path) {
        return defaultHandler;
    }
}

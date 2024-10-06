package segeraroot.connectivity.http;

import segeraroot.connectivity.http.impl.RequestHandler;

public interface EndpointCallback {
    RequestHandler route(HttpMethod method, String path);
}

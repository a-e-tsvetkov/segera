package segeraroot.connectivity.http;

import segeraroot.connectivity.http.impl.RequestHandler;

public interface RequestDispatcher {
    RequestHandler route(HttpMethod method, HttpPath path);
}

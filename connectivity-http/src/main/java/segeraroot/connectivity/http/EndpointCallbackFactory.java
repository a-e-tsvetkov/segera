package segeraroot.connectivity.http;

import segeraroot.connectivity.http.impl.RequestHandler;

public class EndpointCallbackFactory {
    public static RequestHandler staticPage(String content) {
        return new StaticPageEndpointCallback(content);
    }
}

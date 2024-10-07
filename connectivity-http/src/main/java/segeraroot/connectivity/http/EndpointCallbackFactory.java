package segeraroot.connectivity.http;

import segeraroot.connectivity.http.impl.RequestHandlerFactory;

public class EndpointCallbackFactory {
    public static RequestHandlerFactory staticPage(String content) {
        return () -> new StaticPageEndpointCallback(content);
    }
}

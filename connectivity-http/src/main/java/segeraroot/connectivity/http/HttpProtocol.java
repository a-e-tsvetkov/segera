package segeraroot.connectivity.http;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import segeraroot.connectivity.Server;
import segeraroot.connectivity.http.impl.HttpServerHandler;

public class HttpProtocol {

    public static ServerBuilder server(EndpointCallback endpointCallback) {
        return new ServerBuilder(endpointCallback);
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ServerBuilder {
        private final EndpointCallback endpointCallback;

        public Server at(int port) {
            return new Server(port, new HttpServerHandler(endpointCallback));
        }
    }

}

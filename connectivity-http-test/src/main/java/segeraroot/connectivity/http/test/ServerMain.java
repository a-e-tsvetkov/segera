package segeraroot.connectivity.http.test;

import segeraroot.connectivity.http.EndpointCallback;
import segeraroot.connectivity.http.EndpointCallbackFactory;
import segeraroot.connectivity.http.HttpProtocol;

public class ServerMain {

    public static final String CONTENT =
            """
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                        <meta charset="UTF-8">
                        <title>Title</title>
                    </head>
                    <body>
                    Hello world!
                    </body>
                    </html>
                    """;

    public static void main(String[] args) {
        var defaultHandler = EndpointCallbackFactory.staticPage(CONTENT);
        var server = HttpProtocol.server(
                new EndpointCallback(defaultHandler)
        ).at(8080);

        server.start();
    }
}

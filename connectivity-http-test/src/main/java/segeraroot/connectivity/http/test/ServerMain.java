package segeraroot.connectivity.http.test;

import segeraroot.connectivity.http.EndpointCallbackFactory;
import segeraroot.connectivity.http.HttpProtocol;

import java.nio.file.Path;

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
                    Not found
                    </body>
                    </html>
                    """;

    public static void main(String[] args) {
        var root = FileSystemEndpointCallbackFactory.builder()
                .root(Path.of("."))
                .defaultHandler(()-> EndpointCallbackFactory.staticPage(CONTENT))
                .build();
        HttpProtocol
                .server(root)
                .at(8080)
                .start();
    }
}

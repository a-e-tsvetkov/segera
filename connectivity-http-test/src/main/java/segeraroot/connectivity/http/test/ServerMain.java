package segeraroot.connectivity.http.test;

import segeraroot.connectivity.http.HttpProtocol;
import segeraroot.connectivity.http.impl.RequestHandlerFactory;

import java.nio.file.Path;

public class ServerMain {
    public static void main(String[] args) {
        RequestHandlerFactory notFoundPage = StaticContentBuilder.getStaticPage("Error", "Not found");
        var files = FileSystemRequest.builder()
            .fileRoot(Path.of("."))
            .defaultHandler(notFoundPage)
            .build();
        var root = CompositeRequestDispatcher.builder()
            .notFoundHandler(notFoundPage)
            .child(b -> b
                .defaultHandler()
                .customChild("files", files::attach)
                .child("api", b2 -> b2
                    .defaultHandler()
                    .child("foo",
                        StaticContentBuilder.getStaticPage("Foo", "placeholder - foo"))
                    .child("bar",
                        StaticContentBuilder.getStaticPage("Bar", "placeholder - bar")))
                .build())
            .build();
        HttpProtocol
            .server(root)
            .at(8080)
            .start();
    }
}

package segeraroot.connectivity.http.test;

import segeraroot.connectivity.http.HttpProtocol;
import segeraroot.connectivity.http.impl.RequestHandlerFactory;

import java.nio.file.Path;

public class ServerMain {
    public static void main(String[] args) {
        RequestHandlerFactory notFoundPage = StaticContentBuilder.getStaticPage("Error", "Not found");
        var files = FileSystemRequestDispatcher.builder()
                .root(Path.of("."))
                .defaultHandler(notFoundPage)
                .build();
        var root = CompositeRequestDispatcher.builder()
                .notFoundHandler(notFoundPage)
                .child(b -> b
                        .defaultHandler()
                        .child("files", files)
                        .child("api", b2 -> b2
                                .defaultHandler()
                                .child("foo",
                                        StaticContentBuilder.getStaticPage("Foo", "placehlder - foo"))
                                .child("bar",
                                        StaticContentBuilder.getStaticPage("Bar", "placehlder - bar")))
                        .build())
                .build();
        HttpProtocol
                .server(root)
                .at(8080)
                .start();
    }
}

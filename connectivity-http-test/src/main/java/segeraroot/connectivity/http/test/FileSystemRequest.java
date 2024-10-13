package segeraroot.connectivity.http.test;

import lombok.Builder;
import segeraroot.connectivity.http.EndpointCallbackFactory;
import segeraroot.connectivity.http.HttpPath;
import segeraroot.connectivity.http.RequestDispatcher;
import segeraroot.connectivity.http.impl.RequestHandler;
import segeraroot.connectivity.http.impl.RequestHandlerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

@Builder
public class FileSystemRequest{
    private final Path fileRoot;
    private final RequestHandlerFactory defaultHandler;

    public RequestDispatcher attach(HttpPath httpRoot) {
        return (method, path) -> route(path, httpRoot);
    }

    private RequestHandler route(HttpPath path, HttpPath httpRoot) {
        Path current = fileRoot.toAbsolutePath();
        for (int i = 0; i < path.nameCount(); i++) {
            current = current.resolve(path.name(i));
        }
        current = current.normalize();
        if (!Files.exists(current)) {
            return defaultHandler.create();
        }
        if (Files.isDirectory(current)) {
            return directoryContent(current, httpRoot);
        } else {
            return EndpointCallbackFactory.staticPage(CONTENT.formatted(current.toString())).create();
        }
    }

    private RequestHandler directoryContent(Path current, HttpPath httpRoot) {
        try (Stream<Path> list = Files.list(current)){
            return StaticContentBuilder.list(
                    "Content: " + current,
                    list
                            .sorted()
                            .map(p -> Map.entry(
                                    httpRoot.fullName() + "/" + fileRoot.toAbsolutePath().relativize(p),
                                    p.getFileName().toString())));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static final String CONTENT = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <title>Title</title>
            </head>
            <body>
            File: %s
            </body>
            </html>
            """;
}


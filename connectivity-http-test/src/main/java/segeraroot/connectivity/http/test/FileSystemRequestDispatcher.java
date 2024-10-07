package segeraroot.connectivity.http.test;

import lombok.Builder;
import segeraroot.connectivity.http.HttpPath;
import segeraroot.connectivity.http.RequestDispatcher;
import segeraroot.connectivity.http.EndpointCallbackFactory;
import segeraroot.connectivity.http.HttpMethod;
import segeraroot.connectivity.http.impl.RequestHandler;
import segeraroot.connectivity.http.impl.RequestHandlerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Builder
public class FileSystemRequestDispatcher implements RequestDispatcher {
    private final Path root;
    private final RequestHandlerFactory defaultHandler;

    @Override
    public RequestHandler route(HttpMethod method, HttpPath path) {
        Path current = root.toAbsolutePath();
        for (int i = 0; i<path.nameCount();i++){
            current = current.resolve(path.name(i));
        }
        current = current.normalize();
        if (!Files.exists(current)) {
            return defaultHandler.create();
        }
        if (Files.isDirectory(current)) {
            return directoryContent(current);
        } else {
            return EndpointCallbackFactory.staticPage(CONTENT.formatted(current.toString())).create();
        }
    }

    private RequestHandler directoryContent(Path current) {
        try {
            return StaticContentBuilder.list(
                    "Content: " + current,
                    Files.list(current)
                            .sorted()
                            .map(p -> Map.entry(
                                    "/" + root.toAbsolutePath().relativize(p),
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


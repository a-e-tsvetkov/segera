package segeraroot.connectivity.http.test;

import lombok.Builder;
import segeraroot.connectivity.http.EndpointCallback;
import segeraroot.connectivity.http.EndpointCallbackFactory;
import segeraroot.connectivity.http.HttpMethod;
import segeraroot.connectivity.http.impl.RequestHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

public class FileSystemEndpointCallbackFactory {
    public static FileSystemEndpointCallback.FileSystemEndpointCallbackBuilder builder() {
        return FileSystemEndpointCallback.builder();
    }

    @Builder
    private static class FileSystemEndpointCallback implements EndpointCallback {
        private final Path root;
        private final Supplier<RequestHandler> defaultHandler;

        @Override
        public RequestHandler route(HttpMethod method, String path) {
            Path current = root.toAbsolutePath()
                    .resolve(path.substring(1))
                    .normalize();
            if (!Files.exists(current)) {
                return defaultHandler.get();
            }
            if (Files.isDirectory(current)) {
                return EndpointCallbackFactory.staticPage(folderContent(current, root));
            } else {
                return EndpointCallbackFactory.staticPage(CONTENT.formatted(current.toString()));
            }
        }

        private String folderContent(Path current, Path root) {
            StringBuilder sb = new StringBuilder();
            sb.append("""
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                        <meta charset="UTF-8">
                        <title>Content: %s</title>
                    </head>
                    <body>
                    <par>Content:</par>
                    <ul>
                    """.formatted(current.toString()));
            try {
                Files.list(current)
                        .sorted()
                        .forEach(p ->
                                sb.append("<li><a href=\"/")
                                        .append(root.toAbsolutePath().relativize(p))
                                        .append("\">")
                                        .append(p.getFileName())
                                        .append("</a></li>"));
            } catch (IOException e) {
                sb.append(e);
            }
            sb.append("""
                    </ul>
                    </body>
                    </html>
                    """);
            return sb.toString();
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

package segeraroot.connectivity.http.test;

import lombok.experimental.UtilityClass;
import segeraroot.connectivity.http.EndpointCallbackFactory;
import segeraroot.connectivity.http.impl.RequestHandler;
import segeraroot.connectivity.http.impl.RequestHandlerFactory;

import java.util.Map;
import java.util.stream.Stream;

@UtilityClass
public class StaticContentBuilder {
    RequestHandler list(String title, Stream<Map.Entry<String, String>> listElements) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <title>%s</title>
            </head>
            <body>
            <par>Content:</par>
            <ul>
            """.formatted(title));
        listElements
            .forEach(p ->
                sb.append("<li><a href=\"")
                    .append(p.getKey())
                    .append("\">")
                    .append(p.getValue())
                    .append("</a></li>"));
        sb.append("""
            </ul>
            </body>
            </html>
            """);
        String content = sb.toString();
        return EndpointCallbackFactory.staticPage(content).create();
    }

    static RequestHandlerFactory getStaticPage(String title, String text) {
        return EndpointCallbackFactory.staticPage(CONTENT.formatted(title, text));
    }

    private static final String CONTENT =
        """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <title>%s</title>
            </head>
            <body>
            %s
            </body>
            </html>
            """;
}

package segeraroot.connectivity.http.test;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import segeraroot.connectivity.http.HttpMethod;
import segeraroot.connectivity.http.HttpPath;
import segeraroot.connectivity.http.RequestDispatcher;
import segeraroot.connectivity.http.impl.RequestHandler;
import segeraroot.connectivity.http.impl.RequestHandlerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CompositeRequestDispatcher implements RequestDispatcher {
    private final RequestHandlerFactory notFoundPage;
    private final Composite root;

    @Override
    public RequestHandler route(HttpMethod method, HttpPath path) {
        Composite current = root;
        outer:
        for (int i = 0; i < path.nameCount(); i++) {
            String name = path.name(i);
            List<ChildDescriptor> list = current.list;
            //noinspection ForLoopReplaceableByForEach
            for (int j = 0, listSize = list.size(); j < listSize; j++) {
                ChildDescriptor l = list.get(j);
                if (l.name.equals(name)) {
                    if (l.value instanceof Composite subComposite) {
                        current = subComposite;
                        continue outer;
                    }
                    if (l.value instanceof RequestDispatcher dispatcher) {
                        return dispatcher.route(method, path.rest(i));
                    }
                }
            }
            return notFoundPage.create();
        }
        if (current.requestHandlerFactory == null) {
            return notFoundPage.create();
        }
        return current.requestHandlerFactory.create();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private RequestHandlerFactory notFoundPage;
        private Composite root;

        public Builder notFoundHandler(RequestHandlerFactory notFoundPage) {
            this.notFoundPage = notFoundPage;
            return this;
        }

        public Builder child(Consumer<CompositeBuilder> builderConsumer) {
            CompositeBuilder builder = new CompositeBuilder(HttpPath.root());
            builderConsumer.accept(builder);
            root = builder.build();
            return this;
        }

        public CompositeRequestDispatcher build() {
            return new CompositeRequestDispatcher(notFoundPage, root);
        }
    }

    public static class CompositeBuilder {
        private final Composite composite;

        public CompositeBuilder(HttpPath path) {
            composite = new Composite(path);
        }

        public CompositeBuilder handler(RequestHandlerFactory requestHandlerFactory) {
            composite.setContent(requestHandlerFactory);
            return this;
        }

        public CompositeBuilder defaultHandler() {
            return handler(new DefaultHandler(composite));
        }

        public CompositeBuilder child(String name, Consumer<CompositeBuilder> builderConsumer) {
            CompositeBuilder builder = subBuilder(name);
            builderConsumer.accept(builder);
            composite.addChild(new ChildDescriptor(name, builder.build()));
            return this;
        }

        public CompositeBuilder customChild(String name, Function<HttpPath, RequestDispatcher> requestDispatcherFactory) {
            RequestDispatcher requestDispatcher = requestDispatcherFactory.apply(
                composite.path().append(name)
            );
            composite.addChild(new ChildDescriptor(name, requestDispatcher));
            return this;
        }

        public CompositeBuilder child(String name, RequestHandlerFactory page) {
            CompositeBuilder builder = subBuilder(name);
            builder.handler(page);
            composite.addChild(new ChildDescriptor(name, builder.build()));
            return this;
        }

        @SuppressWarnings("ClassEscapesDefinedScope")
        public Composite build() {
            return composite;
        }

        private CompositeBuilder subBuilder(String name) {
            return new CompositeBuilder(composite.path().append(name));
        }
    }

    private record ChildDescriptor(String name, Object value) {
    }

    @RequiredArgsConstructor
    private static class DefaultHandler implements RequestHandlerFactory {
        private final Composite composite;

        @Override
        public RequestHandler create() {
            return StaticContentBuilder.list("Content",
                composite.list
                    .stream()
                    .map(c -> Map.entry(
                        composite.path().append(c.name).fullName(),
                        c.name))
            );
        }
    }

    @RequiredArgsConstructor
    private static class Composite {
        private final HttpPath path;
        private RequestHandlerFactory requestHandlerFactory;
        private final List<ChildDescriptor> list = new ArrayList<>();

        public void setContent(RequestHandlerFactory requestHandlerFactory) {
            this.requestHandlerFactory = requestHandlerFactory;
        }

        public void addChild(ChildDescriptor childDescriptor) {
            list.add(childDescriptor);
        }

        public HttpPath path() {
            return path;
        }
    }
}

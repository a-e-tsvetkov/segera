package segeraroot.connectivity.http;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public sealed class HttpPath {
    public static HttpPath root() {
        return RootHttpPath.ROOT;
    }

    private final String[] parts;
    private final int offset;

    public static HttpPath parse(String pathString) {
        return new HttpPath(pathString.split("/"), 0);
    }

    public int nameCount() {
        return parts.length - offset;
    }

    public String name(int i) {
        return parts[i + offset];
    }

    public HttpPath rest(int i) {
        return new HttpPath(parts, offset + i + 1);
    }

    public HttpPath append(String name) {
        String[] parts = new String[nameCount() + 1];
        for (int i = 0; i < nameCount(); i++) {
            parts[i] = name(i);
        }
        parts[parts.length - 1] = name;
        return new HttpPath(parts, 0);
    }

    public String fullName() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nameCount(); i++) {
            sb.append("/").append(name(i));
        }
        return sb.toString();
    }

    private static final class RootHttpPath extends HttpPath {
        public static final HttpPath ROOT = new RootHttpPath();

        private RootHttpPath() {
            super(new String[0], 0);
        }

        @Override
        public String fullName() {
            return "/";
        }
    }
}

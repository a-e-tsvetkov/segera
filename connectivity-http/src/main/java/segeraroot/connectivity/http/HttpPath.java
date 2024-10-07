package segeraroot.connectivity.http;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class HttpPath {
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
}

package segeraroot.connectivity.http.impl;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class HeaderDecoder {
    private final StringBuilder builder = new StringBuilder();
    @Getter
    private final Map<String, String> header = new HashMap<>();

    public boolean onMessage(ByteStream byteStream) {
        while (true) {
            boolean result = byteStream.readLine(builder);
            if (!result) {
                return false;
            }
            String string = builder.toString();
            if (string.isBlank()) {
                return true;
            }
            builder.setLength(0);
            int index = string.indexOf(":");
            String key = string.substring(0, index);
            String value = string.substring(index + 1).trim();
            header.put(key, value);
        }

    }
}

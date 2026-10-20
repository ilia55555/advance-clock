package com.ilia.advanceclock;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Search and time-zone replies are small JSON documents, never executable downloads. */
final class NetworkResponse {
    private static final int MAX_CHARS = 1_048_576;
    private NetworkResponse() {}

    static String read(InputStream stream) throws IOException {
        StringBuilder result = new StringBuilder();
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            char[] buffer = new char[4096];
            int count;
            while ((count = reader.read(buffer)) != -1) {
                if (result.length() + count > MAX_CHARS) throw new IOException("Response exceeds size limit");
                result.append(buffer, 0, count);
            }
        }
        return result.toString();
    }
}

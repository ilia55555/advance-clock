package com.ilia.advanceclock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.*;

public class NetworkResponseTest {
    @Test public void preservesNormalUtf8Json() throws Exception {
        String json = "{\"name\":\"تهران\"}\n";
        assertEquals(json, NetworkResponse.read(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))));
    }
    @Test public void rejectsOversizedResponse() throws Exception {
        try {
            NetworkResponse.read(new ByteArrayInputStream(new byte[1_048_577]));
            fail("Oversized reply must be rejected");
        } catch (IOException expected) { assertTrue(expected.getMessage().contains("limit")); }
    }
}

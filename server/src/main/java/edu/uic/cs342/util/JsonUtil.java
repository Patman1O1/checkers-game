package edu.uic.cs342.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JsonUtil {

    // ── Fields ────────────────────────────────────────────────────────────────

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    // ── Constructors ──────────────────────────────────────────────────────────

    private JsonUtil() {}

    // ── Methods ───────────────────────────────────────────────────────────────

    public static String toJson(Object obj) {
        try {
            return JsonUtil.MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            return "{\"error\":\"serialisation failed\"}";
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) throws Exception {
        return JsonUtil.MAPPER.readValue(json, clazz);
    }

    public static ObjectMapper getMapper() { return JsonUtil.MAPPER; }
}

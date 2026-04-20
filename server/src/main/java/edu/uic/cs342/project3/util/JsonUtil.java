package edu.uic.cs342.project3.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JsonUtil {
    // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────────
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────
    private JsonUtil() {}

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public static String toJson(Object obj) {
        try {
            return JsonUtil.OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            return "{\"error\":\"serialisation failed\"}";
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) throws JsonProcessingException {
        return JsonUtil.OBJECT_MAPPER.readValue(json, clazz);
    }

    public static ObjectMapper getObjectMapper() { return JsonUtil.OBJECT_MAPPER; }
}

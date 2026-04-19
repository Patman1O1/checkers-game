package edu.uic.cs342.project3.http;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.function.Consumer;

public class Callback {
    // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────────
    public final Consumer<JsonNode> onSuccess;

    public final Consumer<String> onError;

    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────
    public Callback(Consumer<JsonNode> onSuccess, Consumer<String> onError) throws NullPointerException {
        if (onSuccess == null) {
            throw new NullPointerException("onSuccess is null");
        }

        if (onError == null) {
            throw new NullPointerException("onError is null");
        }

        this.onSuccess = onSuccess;
        this.onError = onError;
    }
}

package edu.uic.cs342.project3.http;

public enum HttpMethod {
    // ── Enum Constants ───────────────────────────────────────────────────────────────────────────────────────────────
    GET("GET"), POST("POST");

    // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────────
    private final String name;

    // ── Constructors ───────────────────────────────────────────────────────────────────────────────────────────────
    private HttpMethod(String name) { this.name = name; }

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    @Override
    public String toString() { return this.name; }
}

package edu.uic.cs342.project3.http;

public enum HttpMethod {
    // ── Enum Constants ───────────────────────────────────────────────────────────────────────────────────────────────
    GET,
    POST;

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    @Override
    public String toString() { return this == GET ? "GET" : "POST"; }
}

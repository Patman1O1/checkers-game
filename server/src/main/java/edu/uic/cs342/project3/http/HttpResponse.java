package edu.uic.cs342.project3.http;

import java.io.Serializable;

public class HttpResponse implements Serializable {
    // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────────
    private static final long serialVersionUID = 1L;

    private final int statusCode;

    private final String body;

    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────
    private HttpResponse(int statusCode, String body) {
        this.statusCode = statusCode;
        this.body = body;
    }

    // ── Getters ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public int getStatusCode() { return this.statusCode; }

    public String  getBody() { return this.body; }

    public boolean isOk() { return this.statusCode < 400; }

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public static HttpResponse ok(String json) { return new HttpResponse(200, json); }

    public static HttpResponse badRequest(String message) {
        return new HttpResponse(400, HttpResponse.error(message));
    }

    public static HttpResponse notFound() {
        return new HttpResponse(404, HttpResponse.error("Not found"));
    }

    public static HttpResponse conflict(String message) {
        return new HttpResponse(409, HttpResponse.error(message));
    }

    public static HttpResponse unauthorized(String message) {
        return new HttpResponse(401, HttpResponse.error(message));
    }

    public static HttpResponse serverError(String message) {
        return new HttpResponse(500, HttpResponse.error(message));
    }

    @Override
    public String toString() { return String.format("%d %s", this.statusCode, this.body); }

    private static String error(String message) {
        return String.format("{\"error\":\"%s\"}", message.replace("\"", "'"));
    }
}

package edu.uic.cs342.project3.http;

import java.io.Serializable;

public final class HttpResponse implements Serializable {
    // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────────
    private static final long serialVersionUID = 1L;

    private final long sequenceId;

    private final int statusCode;

    private final String body;

    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────
    private HttpResponse(long sequenceId, int statusCode, String body) {
        this.sequenceId = sequenceId;
        this.statusCode = statusCode;
        this.body = body;
    }

    // ── Getters ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public long getSequenceId() { return this.sequenceId; }

    public int getStatusCode() { return this.statusCode; }

    public String getBody() { return this.body; }

    public boolean isOk() { return this.statusCode < 400; }

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    private static String error(String message) {
        return String.format("{\"error\":\"%s\"}", message.replace("\"", "'"));
    }

    @Override
    public String toString() { return String.format("[%d] %d %s", this.sequenceId, this.statusCode, this.body); }

    public static HttpResponse ok(long sequenceId, String jsonString) {
        return new HttpResponse(sequenceId, HttpStatusCodes.OK, jsonString);
    }

    public static HttpResponse badRequest(long sequenceId, String message) {
        return new HttpResponse(sequenceId, HttpStatusCodes.BAD_REQUEST, HttpResponse.error(message));
    }

    public static HttpResponse notFound(long sequenceId) {
        return new HttpResponse(sequenceId, HttpStatusCodes.NOT_FOUND, HttpResponse.error("Not found"));
    }

    public static HttpResponse conflict(long sequenceId, String message) {
        return new HttpResponse(sequenceId, HttpStatusCodes.CONFLICT, HttpResponse.error(message));
    }

    public static HttpResponse unauthorized(long sequenceId, String message) {
        return new HttpResponse(sequenceId, HttpStatusCodes.UNAUTHORIZED, HttpResponse.error(message));
    }

    public static HttpResponse serverError(long sequenceId, String message) {
        return new HttpResponse(sequenceId, HttpStatusCodes.SERVER_ERROR, HttpResponse.error(message));
    }
}

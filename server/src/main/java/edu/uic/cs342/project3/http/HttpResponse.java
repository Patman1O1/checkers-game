package edu.uic.cs342.project3.http;

import java.io.Serializable;

/**
 * A serialisable response sent from server to client over
 * ObjectInputStream / ObjectOutputStream.
 *
 * sequenceId is copied from the matching HttpRequest so the client can route
 * the response to the correct pending callback.
 */
public class HttpResponse implements Serializable {

    // ── Fields ────────────────────────────────────────────────────────────────

    private static final long serialVersionUID = 1L;

    private final long   sequenceId;
    private final int    statusCode;
    private final String body;

    // ── Constructors ──────────────────────────────────────────────────────────

    private HttpResponse(long sequenceId, int statusCode, String body) {
        this.sequenceId = sequenceId;
        this.statusCode = statusCode;
        this.body       = body;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public long    getSequenceId() { return sequenceId; }
    public int     getStatusCode() { return statusCode; }
    public String  getBody()       { return body;       }
    public boolean isOk()          { return statusCode < 400; }

    // ── Methods ───────────────────────────────────────────────────────────────

    public static HttpResponse ok(long seq, String json) {
        return new HttpResponse(seq, 200, json);
    }

    public static HttpResponse badRequest(long seq, String message) {
        return new HttpResponse(seq, 400, error(message));
    }

    public static HttpResponse notFound(long seq) {
        return new HttpResponse(seq, 404, error("Not found"));
    }

    public static HttpResponse conflict(long seq, String message) {
        return new HttpResponse(seq, 409, error(message));
    }

    public static HttpResponse unauthorized(long seq, String message) {
        return new HttpResponse(seq, 401, error(message));
    }

    public static HttpResponse serverError(long seq, String message) {
        return new HttpResponse(seq, 500, error(message));
    }

    @Override
    public String toString() { return "[" + sequenceId + "] " + statusCode + " " + body; }

    private static String error(String msg) {
        return "{\"error\":\"" + msg.replace("\"", "'") + "\"}";
    }
}

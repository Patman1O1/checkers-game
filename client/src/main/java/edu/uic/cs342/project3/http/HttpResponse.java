package edu.uic.cs342.project3.http;

import java.io.Serializable;

/**
 * Serialisable response POJO received from the server — must match the
 * server-side class exactly (same fields, same serialVersionUID).
 */
public class HttpResponse implements Serializable {

    // ── Fields ────────────────────────────────────────────────────────────────

    private static final long serialVersionUID = 1L;

    private final long   sequenceId;
    private final int    statusCode;
    private final String body;

    // ── Constructors ──────────────────────────────────────────────────────────

    public HttpResponse(long sequenceId, int statusCode, String body) {
        this.sequenceId = sequenceId;
        this.statusCode = statusCode;
        this.body       = body;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public long    getSequenceId() { return sequenceId;      }
    public int     getStatusCode() { return statusCode;      }
    public String  getBody()       { return body;            }
    public boolean isOk()          { return statusCode < 400; }

    // ── Methods ───────────────────────────────────────────────────────────────

    @Override
    public String toString() { return "[" + sequenceId + "] " + statusCode + " " + body; }
}

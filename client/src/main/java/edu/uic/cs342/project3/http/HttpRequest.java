package edu.uic.cs342.project3.http;

import java.io.Serializable;

/**
 * Serialisable request POJO sent to the server — must match the server-side
 * class exactly (same fields, same serialVersionUID).
 */
public class HttpRequest implements Serializable {

    // ── Fields ────────────────────────────────────────────────────────────────

    private static final long serialVersionUID = 1L;

    private final long   sequenceId;
    private final String method;
    private final String path;
    private final String body;

    // ── Constructors ──────────────────────────────────────────────────────────

    public HttpRequest(long sequenceId, String method, String path, String body) {
        this.sequenceId = sequenceId;
        this.method     = method.toUpperCase();
        this.path       = path;
        this.body       = (body != null) ? body : "";
    }

    public HttpRequest(long sequenceId, String method, String path) {
        this(sequenceId, method, path, "");
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public long   getSequenceId() { return sequenceId; }
    public String getMethod()     { return method;     }
    public String getPath()       { return path;       }
    public String getBody()       { return body;       }

    // ── Methods ───────────────────────────────────────────────────────────────

    @Override
    public String toString() { return "[" + sequenceId + "] " + method + " " + path; }
}

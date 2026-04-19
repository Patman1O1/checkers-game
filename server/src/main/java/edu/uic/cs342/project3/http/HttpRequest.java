package edu.uic.cs342.project3.http;

import java.io.Serializable;

public class HttpRequest implements Serializable {
    // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────────
    private static final long serialVersionUID = 1L;

    private final long sequenceId;

    private final HttpMethod method;

    private final String path;

    private final String body;

    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────
    public HttpRequest(long sequenceId, HttpMethod method, String path, String body) throws NullPointerException {
        if (body == null) {
            throw new NullPointerException("body is null");
        }

        this.sequenceId = sequenceId;
        this.method = method;
        this.path = path;
        this.body = body;
    }

    public HttpRequest(long sequenceId, HttpMethod method, String path) {
        this.sequenceId = sequenceId;
        this.method = method;
        this.path = path;
        this.body = "";
    }

    // ── Getters ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public long getSequenceId() { return this.sequenceId; }

    public HttpMethod getMethod() { return this.method; }

    public String getPath() { return this.path; }

    public String getBody() { return this.body; }

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    @Override
    public String toString() { return String.format("[%d] %s %s", this.sequenceId, this.method, this.path); }
}

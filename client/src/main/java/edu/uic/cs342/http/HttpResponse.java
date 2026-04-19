package edu.uic.cs342.http;

import java.io.Serializable;

public class HttpResponse implements Serializable {

    // ── Fields ────────────────────────────────────────────────────────────────

    private static final long serialVersionUID = 1L;

    private final int    statusCode;
    private final String body;

    // ── Constructors ──────────────────────────────────────────────────────────

    public HttpResponse(int statusCode, String body) {
        this.statusCode = statusCode;
        this.body       = body;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public int     getStatusCode() { return this.statusCode;       }
    public String  getBody()       { return this.body;             }
    public boolean isOk()          { return this.statusCode < 400; }

    // ── Methods ───────────────────────────────────────────────────────────────

    @Override
    public String toString() { return this.statusCode + " " + this.body; }
}

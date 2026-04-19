package edu.uic.cs342.http;

import java.io.Serializable;

public class HttpRequest implements Serializable {

    // ── Fields ────────────────────────────────────────────────────────────────

    private static final long serialVersionUID = 1L;

    private final String method;
    private final String path;
    private final String body;

    // ── Constructors ──────────────────────────────────────────────────────────

    public HttpRequest(String method, String path, String body) {
        this.method = method.toUpperCase();
        this.path   = path;
        this.body   = (body != null) ? body : "";
    }

    public HttpRequest(String method, String path) {
        this(method, path, "");
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getMethod() { return this.method; }
    public String getPath()   { return this.path;   }
    public String getBody()   { return this.body;   }

    // ── Methods ───────────────────────────────────────────────────────────────

    @Override
    public String toString() { return this.method + " " + this.path; }
}

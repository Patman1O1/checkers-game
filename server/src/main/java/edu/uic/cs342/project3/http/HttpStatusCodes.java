package edu.uic.cs342.project3.http;

public final class HttpStatusCodes {
    // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────────
    public static final HttpStatusCode OK = new HttpStatusCode(200, "Ok");

    public static final HttpStatusCode BAD_REQUEST = new HttpStatusCode(400, "Bad Request");

    public static final HttpStatusCode NOT_FOUND = new HttpStatusCode(404, "Not Found");

    public static final HttpStatusCode CONFLICT = new HttpStatusCode(409, "Conflict");

    public static final HttpStatusCode UNAUTHORIZED = new HttpStatusCode(401, "Unauthorized");

    public static final HttpStatusCode SERVER_ERROR = new HttpStatusCode(500, "Server Error");

    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────
    private HttpStatusCodes() {}
}

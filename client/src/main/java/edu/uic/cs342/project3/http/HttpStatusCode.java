package edu.uic.cs342.project3.http;

import java.io.Serializable;

public class HttpStatusCode implements Serializable {
    // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────────
    private final int value;

    private final String name;

    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────
    public HttpStatusCode(int value, String name) throws NullPointerException, IllegalArgumentException {
        if (value > 599 || value < 100) {
            throw new IllegalArgumentException(String.format("Expected value to be between 599 and 100 (inclusive) but got %d", value));
        }

        if (name == null) {
            throw new NullPointerException("name is null");
        }

        this.value = value;
        this.name = name;
    }

    // ── Getters ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public int getValue() { return this.value; }

    public String getName() { return this.name; }

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    @Override
    public boolean equals(Object object) {
        if (!(object instanceof HttpStatusCode)) {
            return false;
        }
        HttpStatusCode statusCode = (HttpStatusCode) object;
        return this.value == statusCode.value;
    }

    @Override
    public String toString() { return this.name; }
}

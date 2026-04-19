package edu.uic.cs342.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Color {

    // ── Constants ─────────────────────────────────────────────────────────────

    RED("red"),
    BLACK("black");

    // ── Fields ────────────────────────────────────────────────────────────────

    private final String value;

    // ── Constructors ──────────────────────────────────────────────────────────

    Color(String value) {
        this.value = value;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    @JsonValue
    public String getValue() { return this.value; }

    // ── Methods ───────────────────────────────────────────────────────────────

    @JsonCreator
    public static Color fromValue(String value) {
        for (Color c : Color.values())
            if (c.value.equalsIgnoreCase(value)) return c;
        throw new IllegalArgumentException("Unknown color: " + value);
    }

    public Color opposite() {
        return this == Color.RED ? Color.BLACK : Color.RED;
    }

    @Override
    public String toString() { return this.value; }
}

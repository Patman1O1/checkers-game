package edu.uic.cs342.project3.model;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Represents the two piece colors in checkers.
 * Serialises to/from lowercase JSON strings ("red", "black") so the
 * wire format is unchanged and the client needs no modifications.
 */
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

    /** Returns the lowercase JSON string ("red" or "black"). */
    @JsonValue
    public String getValue() { return value; }

    // ── Methods ───────────────────────────────────────────────────────────────

    /** Deserialises from the lowercase JSON string produced by {@link #getValue()}. */
    @JsonCreator
    public static Color fromValue(String value) {
        for (Color c : values())
            if (c.value.equalsIgnoreCase(value)) return c;
        throw new IllegalArgumentException("Unknown color: " + value);
    }

    /** Returns the opposing color. */
    public Color opposite() {
        return this == RED ? BLACK : RED;
    }

    @Override
    public String toString() { return value; }
}

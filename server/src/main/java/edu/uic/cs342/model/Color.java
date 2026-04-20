package edu.uic.cs342.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Color {
    // ── Constants ─────────────────────────────────────────────────────────────
    RED("red"), BLACK("black");

    // ── Fields ────────────────────────────────────────────────────────────────
    private final String value;

    // ── Constructors ──────────────────────────────────────────────────────────
    private Color(String value) { this.value = value; }

    // ── Getters ───────────────────────────────────────────────────────────────
    @JsonValue
    public String getValue() { return this.value; }

    // ── Methods ───────────────────────────────────────────────────────────────
    @Override
    public String toString() { return this.value; }

    @JsonCreator
    public static Color fromValue(String value) throws IllegalArgumentException {
        for (Color color : Color.values()) {
            if (color.value.equalsIgnoreCase(value)) {
                return color;
            }
        }
        throw new IllegalArgumentException(String.format("Unknown color: %s", value));
    }

    public Color opposite() { return this == Color.RED ? Color.BLACK : Color.RED; }
}

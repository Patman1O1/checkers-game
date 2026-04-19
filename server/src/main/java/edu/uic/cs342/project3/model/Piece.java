package edu.uic.cs342.project3.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Piece {
    // ── Type ─────────────────────────────────────────────────────────────────────────────────────────────────────────
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static enum Type {
        // ── Constants ────────────────────────────────────────────────────────────────────────────────────────────────
        REGULAR("regular"),
        KING("king");

        // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────
        private final String name;

        // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────
        private Type(String name) { this.name = name; }

        // ── Getters ──────────────────────────────────────────────────────────────────────────────────────────────────
        @JsonValue
        public String getName() { return this.name; }

        // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────
        @JsonCreator
        public static Type fromName(String name) throws IllegalArgumentException {
            for (Type type: Type.values()) {
                if (type.name.equalsIgnoreCase(name)) {
                    return type;
                }
            }
            throw new IllegalArgumentException(String.format("Unknown type: %s", name));
        }

        @Override
        public String toString() { return this.name; }
    }

    // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────────
    @JsonProperty("color")
    public final Color color;

    @JsonProperty("type")
    public final Type type;

    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────
    public Piece(Color color, Type type) {
        this.color = color;
        this.type = type;
    }
}

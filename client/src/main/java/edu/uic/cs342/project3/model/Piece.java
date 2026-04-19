package edu.uic.cs342.project3.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import edu.uic.cs342.project3.json.deserializers.PieceDeserializer;
import edu.uic.cs342.project3.json.serializers.PieceSerializer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(using = PieceSerializer.class)
@JsonDeserialize(using = PieceDeserializer.class)
public class Piece {
    // ── Type ─────────────────────────────────────────────────────────────────────────────────────────────────────────
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static enum Type {
        // ── Constants ────────────────────────────────────────────────────────────────────────────────────────────────
        REGULAR("regular"), KING("king");

        // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────
        @JsonProperty("name")
        private final String name;

        // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────
        private Type(String name) { this.name = name; }

        // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────
        @Override
        public String toString() { return this.name; }
    }

    // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────────
    @JsonProperty("color")
    private Color color;

    @JsonProperty("type")
    private Type type;

    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────
    public Piece() {
        this.type = null;
        this.color = null;
    }

    public Piece(Color color, Type type) throws NullPointerException {
        if (color == null) {
            throw new NullPointerException("color is null");
        }

        if (type == null) {
            throw new NullPointerException("type is null");
        }

        this.color = color;
        this.type = type;
    }

    // ── Setters ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public void setColor(Color color) throws NullPointerException {
        if (color == null) {
            throw new NullPointerException("color is null");
        }
        this.color = color;
    }

    public void setType(Type type) throws NullPointerException {
        if (type == null) {
            throw new NullPointerException("type is null");
        }
        this.type = type;
    }

    // ── Getters ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public Color getColor() { return this.color; }

    public Type getType() { return this.type; }
}

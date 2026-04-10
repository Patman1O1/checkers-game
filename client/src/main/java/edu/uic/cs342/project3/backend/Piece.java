package edu.uic.cs342.project3.backend;

public class Piece {
    // ── Type ─────────────────────────────────────────────────────────────────────────────────────────────────────────
    public static enum Type {
        // ── Enum Constants ───────────────────────────────────────────────────────────────────────────────────────────
        REGULAR,
        KING
    }

    // ── Color ────────────────────────────────────────────────────────────────────────────────────────────────────────
    public static enum Color {
        // ── Enum Constants ───────────────────────────────────────────────────────────────────────────────────────────
        RED,
        BLACK
    }

    // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────────
    private final Color color;

    private Type type;

    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────
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
    public void setType(Type type) throws NullPointerException {
        if (type == null) {
            throw new NullPointerException("type is null");
        }
        this.type = type;
    }

    // ── Getters ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public Type getType() { return this.type; }

    public Color getColor() { return this.color; }

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────

}


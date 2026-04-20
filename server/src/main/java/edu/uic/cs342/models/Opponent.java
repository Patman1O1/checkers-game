package edu.uic.cs342.model;

public interface Opponent {
    // ── Getters ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public String getName();

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public String applyMove(Board board, Color color, Color currentTurn, Board.Pos from, Board.Pos to);
}

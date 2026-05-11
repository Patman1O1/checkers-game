package edu.uic.cs342.project3.models;

public interface Opponent {
    // ── Getters ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public String getName();

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    // Applies a move to the board; returns an error message on failure, null on success
    // For Player: validates and applies the human's chosen from/to positions
    // For AiPlayer: ignores from/to and calculates its own best move
    public String applyMove(Board board, Color color, Color currentTurn, Board.Pos from, Board.Pos to);
}

package edu.uic.cs342.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class Board {

    // ── Subclasses / Subrecords ───────────────────────────────────────────────

    public static class Piece {

        // ── Fields ────────────────────────────────────────────────────────────

        @JsonProperty("color")  private Color   color;
        @JsonProperty("king")   private boolean king;

        // ── Constructors ──────────────────────────────────────────────────────

        public Piece() {}

        public Piece(Color color, boolean king) {
            this.color = color;
            this.king  = king;
        }

        // ── Setters ───────────────────────────────────────────────────────────

        public void setKing(boolean k) { this.king = k; }

        // ── Getters ───────────────────────────────────────────────────────────

        public Color   getColor() { return this.color; }
        public boolean isKing()   { return this.king;  }

        // ── Methods ───────────────────────────────────────────────────────────

        public Piece copy() { return new Piece(this.color, this.king); }

        @Override
        public String toString() {
            return (this.king ? "K" : "") + this.color.toString().substring(0, 1).toUpperCase();
        }
    }

    public record Pos(int row, int col) {}

    public record Move(Pos from, Pos to) {}

    // ── Fields ────────────────────────────────────────────────────────────────

    @JsonProperty("grid")
    private Piece[][] grid;

    // ── Constructors ──────────────────────────────────────────────────────────

    public Board() {
        this.grid = Board.init();
    }

    private Board(Piece[][] grid) {
        this.grid = grid;
    }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setGrid(Piece[][] g) { this.grid = g; }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Piece[][] getGrid()      { return this.grid;                          }
    public Piece     pieceAt(Pos p) { return this.grid[p.row()][p.col()]; }

    // ── Methods ───────────────────────────────────────────────────────────────

    public String applyMove(Pos from, Pos to, Color playerColor, Color currentTurn) {
        if (!Board.inBounds(from) || !Board.inBounds(to))
            return "Position out of bounds.";

        Piece piece = this.grid[from.row()][from.col()];
        if (piece == null)
            return "No piece at source position.";

        if (piece.getColor() != playerColor)
            return "That is not your piece.";

        if (currentTurn != playerColor)
            return "It is not your turn.";

        List<Move> valid = this.validMoves(playerColor);
        if (valid.stream().noneMatch(m -> m.from().equals(from) && m.to().equals(to)))
            return "Invalid move.";

        Board.executeMove(this.grid, from, to, piece);
        return null;
    }

    public List<Move> validMoves(Color color) {
        return Board.getAllValidMoves(this.grid, color);
    }

    public String checkOutcome() {
        return Board.checkOutcome(this.grid);
    }

    public Piece[][] copyGrid() {
        return Board.copyGrid(this.grid);
    }

    public Board copy() {
        return new Board(Board.copyGrid(this.grid));
    }

    // ── Package-accessible statics used by AiPlayer ───────────────────────────

    static List<Move> getAllValidMoves(Piece[][] b, Color color) {
        List<Move> jumps   = new ArrayList<>();
        List<Move> regular = new ArrayList<>();

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = b[r][c];
                if (p == null || p.getColor() != color) continue;
                Pos pos = new Pos(r, c);
                jumps.addAll(Board.getJumps(b, pos, p));
                regular.addAll(Board.getRegularMoves(b, pos, p));
            }
        }
        return jumps.isEmpty() ? regular : jumps;
    }

    static void executeMove(Piece[][] b, Pos from, Pos to, Piece piece) {
        b[to.row()][to.col()]     = piece;
        b[from.row()][from.col()] = null;

        int dr = to.row() - from.row();
        int dc = to.col() - from.col();
        if (Math.abs(dr) == 2)
            b[from.row() + dr / 2][from.col() + dc / 2] = null;

        if (piece.getColor() == Color.RED   && to.row() == 0) piece.setKing(true);
        if (piece.getColor() == Color.BLACK && to.row() == 7) piece.setKing(true);
    }

    static Piece[][] copyGrid(Piece[][] src) {
        Piece[][] copy = new Piece[8][8];
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                copy[r][c] = src[r][c] == null ? null : src[r][c].copy();
        return copy;
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private static List<Move> getRegularMoves(Piece[][] b, Pos from, Piece p) {
        List<Move> moves = new ArrayList<>();
        for (int[] d : Board.directions(p)) {
            int nr = from.row() + d[0];
            int nc = from.col() + d[1];
            Pos to = new Pos(nr, nc);
            if (Board.inBounds(to) && b[nr][nc] == null)
                moves.add(new Move(from, to));
        }
        return moves;
    }

    private static List<Move> getJumps(Piece[][] b, Pos from, Piece p) {
        List<Move> jumps = new ArrayList<>();
        for (int[] d : Board.directions(p)) {
            int mr = from.row() + d[0],     mc = from.col() + d[1];
            int nr = from.row() + d[0] * 2, nc = from.col() + d[1] * 2;
            Pos to = new Pos(nr, nc);
            if (!Board.inBounds(to)) continue;
            Piece mid = b[mr][mc];
            if (mid != null && mid.getColor() != p.getColor() && b[nr][nc] == null)
                jumps.add(new Move(from, to));
        }
        return jumps;
    }

    private static int[][] directions(Piece p) {
        if (p.isKing()) return new int[][]{{-1,-1},{-1,1},{1,-1},{1,1}};
        return p.getColor() == Color.RED
                ? new int[][]{{-1,-1},{-1,1}}
                : new int[][]{{1,-1},{1,1}};
    }

    private static String checkOutcome(Piece[][] b) {
        boolean hasRed = false, hasBlack = false;
        for (Piece[] row : b)
            for (Piece p : row)
                if (p != null) {
                    if (p.getColor() == Color.RED)   hasRed   = true;
                    if (p.getColor() == Color.BLACK) hasBlack = true;
                }

        if (!hasRed)   return Color.BLACK.getValue();
        if (!hasBlack) return Color.RED.getValue();

        boolean redCanMove   = !Board.getAllValidMoves(b, Color.RED).isEmpty();
        boolean blackCanMove = !Board.getAllValidMoves(b, Color.BLACK).isEmpty();

        if (!redCanMove && !blackCanMove) return "draw";
        if (!redCanMove)   return Color.BLACK.getValue();
        if (!blackCanMove) return Color.RED.getValue();
        return null;
    }

    private static boolean inBounds(Pos p) {
        return p.row() >= 0 && p.row() < 8 && p.col() >= 0 && p.col() < 8;
    }

    private static Piece[][] init() {
        Piece[][] b = new Piece[8][8];
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 8; c++)
                if ((r + c) % 2 == 1) b[r][c] = new Piece(Color.BLACK, false);
        for (int r = 5; r < 8; r++)
            for (int c = 0; c < 8; c++)
                if ((r + c) % 2 == 1) b[r][c] = new Piece(Color.RED, false);
        return b;
    }
}

package edu.uic.cs342.project3.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class Board {
    // ── Piece ────────────────────────────────────────────────────────────────────────────────────────────────────────
    public static class Piece {
        // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────
        @JsonProperty("color")
        private Color color;

        @JsonProperty("king")
        private boolean king;

        // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────
        public Piece() {}

        public Piece(Color color, boolean king) {
            this.color = color;
            this.king = king;
        }

        // ── Setters ──────────────────────────────────────────────────────────────────────────────────────────────────
        public void setKing(boolean king) { this.king = king; }

        // ── Getters ──────────────────────────────────────────────────────────────────────────────────────────────────
        public Color getColor() { return this.color; }

        public boolean isKing() { return this.king; }

        // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────
        public Piece copy() { return new Piece(this.color, this.king); }

        @Override
        public String toString() {
            return (this.king ? "K" : "") + this.color.toString().substring(0, 1).toUpperCase();
        }
    }

    public static class Pos {
        public final int rowNum, colNum;

        public Pos(int rowNum, int colNum) {
            this.rowNum = rowNum;
            this.colNum = colNum;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Pos)) return false;
            Pos other = (Pos) obj;
            return this.rowNum == other.rowNum && this.colNum == other.colNum;
        }

        @Override
        public int hashCode() {
            return 31 * this.rowNum + this.colNum;
        }
    }

    public static class Move {
        public final Pos from, to;

        public Move(Pos from, Pos to) {
            this.from = from;
            this.to = to;
        }
    }

    // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────────
    @JsonProperty("grid")
    private Piece[][] grid;

    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────
    public Board() { this.grid = Board.init(); }

    private Board(Piece[][] grid) { this.grid = grid; }

    // ── Setters ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public void setGrid(Piece[][] grid) { this.grid = grid; }

    // ── Getters ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public Piece[][] getGrid() { return this.grid; }

    public Piece pieceAt(Pos position) { return this.grid[position.rowNum][position.colNum]; }

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    private static List<Move> getRegularMoves(Piece[][] board, Pos from, Piece piece) {
        List<Move> moves = new ArrayList<>();
        for (int[] direction : Board.directions(piece)) {
            int nextRowNum = from.rowNum + direction[0];
            int nextColNum = from.colNum + direction[1];

            Pos to = new Pos(nextRowNum, nextColNum);

            if (Board.inBounds(to) && board[nextRowNum][nextColNum] == null) {
                moves.add(new Move(from, to));
            }
        }
        return moves;
    }

    private static List<Move> getJumps(Piece[][] board, Pos from, Piece piece) {
        List<Move> jumps = new ArrayList<>();
        for (int[] direction : Board.directions(piece)) {
            int middleRowNum = from.rowNum + direction[0];
            int middleColNum = from.colNum + direction[1];

            int nextRowNum = from.rowNum + direction[0] * 2;
            int nextColNum = from.colNum + direction[1] * 2;

            Pos to = new Pos(nextRowNum, nextColNum);
            if (!Board.inBounds(to)) {
                continue;
            }
            Piece middlePiece = board[middleRowNum][middleColNum];
            if (middlePiece != null && middlePiece.getColor() != piece.getColor() && board[nextRowNum][nextColNum] == null) {
                jumps.add(new Move(from, to));
            }
        }
        return jumps;
    }

    private static int[][] directions(Piece piece) {
        if (piece.isKing()) {
            return new int[][]{{-1,-1},{-1,1},{1,-1},{1,1}};
        }
        return piece.getColor() == Color.RED
                ? new int[][]{{-1,-1},{-1,1}}
                : new int[][]{{1,-1},{1,1}};
    }

    private static String checkOutcome(Piece[][] board) {
        boolean hasRed = false, hasBlack = false;
        for (Piece[] row : board) {
            for (Piece piece : row)
                if (piece != null) {
                    if (piece.getColor() == Color.RED) {
                        hasRed = true;
                    }

                    if (piece.getColor() == Color.BLACK) {
                        hasBlack = true;
                    }
                }
        }

        if (!hasRed) {
            return Color.BLACK.getValue();
        }
        if (!hasBlack) {
            return Color.RED.getValue();
        }

        boolean redCanMove = !Board.getAllValidMoves(board, Color.RED).isEmpty();
        boolean blackCanMove = !Board.getAllValidMoves(board, Color.BLACK).isEmpty();

        if (!redCanMove && !blackCanMove) {
            return "draw";
        }

        if (!redCanMove) {
            return Color.BLACK.getValue();
        }

        if (!blackCanMove) {
            return Color.RED.getValue();
        }
        return null;
    }

    private static boolean inBounds(Pos pos) {
        return pos.rowNum >= 0 && pos.rowNum < 8 && pos.colNum >= 0 && pos.colNum < 8;
    }

    private static Piece[][] init() {
        Piece[][] board = new Piece[8][8];
        for (int rowNum = 0; rowNum < 3; ++rowNum) {
            for (int colNum = 0; colNum < 8; ++colNum) {
                if ((rowNum + colNum) % 2 == 1) {
                    board[rowNum][colNum] = new Piece(Color.BLACK, false);
                }
            }
        }

        for (int rowNum = 5; rowNum < 8; ++rowNum) {
            for (int colNum = 0; colNum < 8; ++colNum) {
                if ((rowNum + colNum) % 2 == 1) {
                    board[rowNum][colNum] = new Piece(Color.RED, false);
                }
            }
        }
        return board;
    }

    protected static List<Move> getAllValidMoves(Piece[][] board, Color color) {
        List<Move> jumps   = new ArrayList<>();
        List<Move> regular = new ArrayList<>();

        for (int rowNum = 0; rowNum < 8; ++rowNum) {
            for (int colNum = 0; colNum < 8; ++colNum) {
                Piece piece = board[rowNum][colNum];
                if (piece == null || piece.getColor() != color) {
                    continue;
                }
                Pos pos = new Pos(rowNum, colNum);
                jumps.addAll(Board.getJumps(board, pos, piece));
                regular.addAll(Board.getRegularMoves(board, pos, piece));
            }
        }
        return jumps.isEmpty() ? regular : jumps;
    }

    protected static void executeMove(Piece[][] board, Pos from, Pos to, Piece piece) {
        board[to.rowNum][to.colNum] = piece;
        board[from.rowNum][from.colNum] = null;

        int rowDist = to.rowNum - from.rowNum;
        int colDist = to.colNum - from.colNum;

        if (Math.abs(rowDist) == 2) {
            board[from.rowNum + rowDist / 2][from.colNum + colDist / 2] = null;
        }

        if (piece.getColor() == Color.RED && to.rowNum == 0) {
            piece.setKing(true);
        }

        if (piece.getColor() == Color.BLACK && to.rowNum == 7) {
            piece.setKing(true);
        }
    }

    protected static Piece[][] copyGrid(Piece[][] src) {
        Piece[][] copy = new Piece[8][8];
        for (int rowNum = 0; rowNum < 8; ++rowNum) {
            for (int colNum = 0; colNum < 8; ++colNum) {
                copy[rowNum][colNum] = src[rowNum][colNum] == null ? null : src[rowNum][colNum].copy();
            }
        }
        return copy;
    }

    public String applyMove(Pos from, Pos to, Color playerColor, Color currentTurn) {
        if (!Board.inBounds(from) || !Board.inBounds(to))
            return "Position out of bounds.";

        Piece piece = this.grid[from.rowNum][from.colNum];
        if (piece == null)
            return "No piece at source position.";

        if (piece.getColor() != playerColor)
            return "That is not your piece.";

        if (currentTurn != playerColor)
            return "It is not your turn.";

        List<Move> valid = this.validMoves(playerColor);
        if (valid.stream().noneMatch(move -> move.from.equals(from) && move.to.equals(to))) {
            return "Invalid move.";
        }

        Board.executeMove(this.grid, from, to, piece);
        return null;
    }

    public List<Move> validMoves(Color color) { return Board.getAllValidMoves(this.grid, color); }

    public String checkOutcome() { return Board.checkOutcome(this.grid); }

    public Piece[][] copyGrid() { return Board.copyGrid(this.grid); }

    public Board copy() { return new Board(Board.copyGrid(this.grid)); }
}
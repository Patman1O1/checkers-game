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
            /*
            * KR = Red King Piece
            * KB = Black King Piece
            * R = Red Regular Piece
            * B = Black Regular Piece
            * */
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
            // Return true if both the address of this and obj point to the same Pos instance
            if (this == obj) return true;

            // Return false if obj is not of type Pos
            if (!(obj instanceof Pos)) return false;

            // Cast obj to Pos
            Pos other = (Pos) obj;

            // Return true if both the row numbers and column numbers are equal
            return this.rowNum == other.rowNum && this.colNum == other.colNum;
        }

        // Java has a contract which states that class definitions
        // that override equals() must also override hashCode()
        @Override
        public int hashCode() {
            // Use 31 as the part of the hash code since it is odd, prime, and the JVM can optimize
            // 31 * x into (x << 5) - x (where x is this.rowNum + this.colNum)
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

    // Default constructor needed for Jackson
    private Board(Piece[][] grid) { this.grid = grid; }

    // ── Setters ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public void setGrid(Piece[][] grid) { this.grid = grid; }

    // ── Getters ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public Piece[][] getGrid() { return this.grid; }

    public Piece pieceAt(Pos position) { return this.grid[position.rowNum][position.colNum]; }

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    // Returns all single-step diagonal moves for the piece at 'from' (not jumps)
    private static List<Move> getRegularMoves(Piece[][] board, Pos from, Piece piece) {
        List<Move> moves = new ArrayList<>();

        // For each diagonal direction the piece can take...
        for (int[] direction : Board.directions(piece)) {
            // Get the next position
            int nextRowNum = from.rowNum + direction[0];
            int nextColNum = from.colNum + direction[1];
            Pos to = new Pos(nextRowNum, nextColNum);

            // If the move is in bounds and to an empty square...
            if (Board.inBounds(to) && board[nextRowNum][nextColNum] == null) {
                // Add the move to the list of moves
                moves.add(new Move(from, to));
            }
        }

        return moves;
    }

    // Returns all jump moves for the piece at position "from"
    private static List<Move> getJumps(Piece[][] board, Pos from, Piece piece) {
        List<Move> jumps = new ArrayList<>();

        // For each direction the piece can take...
        for (int[] direction : Board.directions(piece)) {
            // Get the position two squares from the current piece
            int nextRowNum = from.rowNum + direction[0] * 2; // Multiply by two because we are moving two squares diagonally
            int nextColNum = from.colNum + direction[1] * 2;
            Pos to = new Pos(nextRowNum, nextColNum);

            // If next position is out of bounds...
            if (!Board.inBounds(to)) {
                // Continue to the next possible direction
                continue;
            }

            // Get the position of the square in between the "from" and "to"
            int middleRowNum = from.rowNum + direction[0];
            int middleColNum = from.colNum + direction[1];
            Piece middlePiece = board[middleRowNum][middleColNum];

            // If the square at (middleRowNum, middleColNum) has a piece that belongs to the opponent
            // and the square at position "to" has no piece on it (i.e. the landing square is empty)...
            if (middlePiece != null && middlePiece.getColor() != piece.getColor() && board[nextRowNum][nextColNum] == null) {
                // Add the move to the list of valid jumps
                jumps.add(new Move(from, to));
            }
        }
        return jumps;
    }

    private static int[][] directions(Piece piece) {
        /*
        Directions Key
        {-1, -1} up-left
        {-1, +1} up-right
        {+1, -1} down-left
        {+1, +1} down-right
        */

        if (piece.isKing()) {
            // Kings move in all 4 diagonal directions
            return new int[][]{{-1,-1},{-1,1},{1,-1},{1,1}};
        }
        return piece.getColor() == Color.RED
                ? new int[][]{{-1,-1},{-1,1}} // Red: up-left and up-right only
                : new int[][]{{1,-1},{1,1}}; // Black: down-left and down-right only
    }

    private static String checkOutcome(Piece[][] board) {
        boolean hasRed = false, hasBlack = false;
        // For each row on the board...
        for (Piece[] row : board) {
            // For each square in the current row...
            for (Piece piece : row) {
                // If the square has a piece...
                if (piece != null) {
                    // If that piece is red...
                    if (piece.getColor() == Color.RED) {
                        hasRed = true;
                    }

                    // If that piece is black...
                    if (piece.getColor() == Color.BLACK) {
                        hasBlack = true;
                    }
                }
            }
        }

        // If all red pieces have been captured by the player with black pieces...
        if (!hasRed) {
            // The player with the black pieces wins
            return Color.BLACK.getValue();
        }

        // If all red pieces have been captured by the player with black pieces...
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
        List<Move> jumps = new ArrayList<>();
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
}
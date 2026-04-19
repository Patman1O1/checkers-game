package edu.uic.cs342.project3.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the 8×8 checkers board.
 *
 * Responsible for:
 *   - Piece placement and initialisation
 *   - Move validation (regular moves, mandatory jumps)
 *   - Move execution (piece movement, captures, king promotion)
 *   - Win / draw detection
 *   - Board copying for AI simulation
 *   - Minimax with alpha-beta pruning (AI search)
 */
public class Board {

    // ── Subclasses / Subrecords ───────────────────────────────────────────────

    /** A single checkers piece. */
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

        public Color   getColor() { return color; }
        public boolean isKing()   { return king;  }

        // ── Methods ───────────────────────────────────────────────────────────

        public Piece copy() { return new Piece(color, king); }

        @Override
        public String toString() {
            return (king ? "K" : "") + color.toString().substring(0, 1).toUpperCase();
        }
    }

    /** A position on the board. */
    public record Pos(int row, int col) {}

    /** A move from one position to another. */
    public record Move(Pos from, Pos to) {}

    /** Internal minimax result — best move paired with its score. */
    private record MinimaxResult(Move move, int score) {}

    // ── Fields ────────────────────────────────────────────────────────────────

    private static final int   AI_DEPTH     = 5;
    private static final Color AI_COLOR     = Color.BLACK;
    private static final Color PLAYER_COLOR = Color.RED;

    @JsonProperty("grid")
    private Piece[][] grid;

    // ── Constructors ──────────────────────────────────────────────────────────

    /** Creates a fresh board with pieces in their starting positions. */
    public Board() {
        this.grid = init();
    }

    private Board(Piece[][] grid) {
        this.grid = grid;
    }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setGrid(Piece[][] g) { this.grid = g; }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Piece[][] getGrid()      { return grid;                       }
    public Piece     pieceAt(Pos p) { return grid[p.row()][p.col()]; }

    // ── Methods ───────────────────────────────────────────────────────────────

    /**
     * Attempt to execute a move for the given player colour.
     * @return null on success, or a non-null error string on failure
     */
    public String applyMove(Pos from, Pos to, Color playerColor, Color currentTurn) {
        if (!inBounds(from) || !inBounds(to))
            return "Position out of bounds.";

        Piece piece = grid[from.row()][from.col()];
        if (piece == null)
            return "No piece at source position.";

        if (piece.getColor() != playerColor)
            return "That is not your piece.";

        if (currentTurn != playerColor)
            return "It is not your turn.";

        List<Move> valid = validMoves(playerColor);
        if (valid.stream().noneMatch(m -> m.from().equals(from) && m.to().equals(to))) {
            System.out.printf("Current Position: (%d, %d)\nRequested Position: (%d, %d)\n", from.row, from.col, to.row, to.col);
            int i = 1;
            for (Move validMove : valid) {
                System.out.printf("Valid Move %d: (%d, %d)\n", i, validMove.to.row, validMove.to.col);
                ++i;
            }
            return "Invalid move.";
        }

        executeMove(grid, from, to, piece);
        return null;
    }

    /**
     * Returns all legal moves for the given color.
     * If any jump is available, only jumps are returned (mandatory capture rule).
     */
    public List<Move> validMoves(Color color) {
        return getAllValidMoves(grid, color);
    }

    /**
     * Check whether the game has ended after the most recent move.
     * @return Color.RED, Color.BLACK, the string "draw", or null if still in progress
     */
    public String checkOutcome() {
        return checkOutcome(grid);
    }

    /**
     * Run the AI and return the best move for the AI color (BLACK).
     * Returns null if the AI has no moves (caller should end the game).
     */
    public Move bestAiMove() {
        Piece[][] copy = copy(grid);
        return minimax(copy, AI_DEPTH, Integer.MIN_VALUE, Integer.MAX_VALUE, true).move;
    }

    /** Returns a deep copy of this board for use in AI simulation. */
    public Board copy() {
        return new Board(copy(grid));
    }

    // ── Private — move execution and generation ───────────────────────────────

    private static void executeMove(Piece[][] b, Pos from, Pos to, Piece piece) {
        b[to.row()][to.col()]     = piece;
        b[from.row()][from.col()] = null;

        int dr = to.row() - from.row();
        int dc = to.col() - from.col();
        if (Math.abs(dr) == 2)
            b[from.row() + dr / 2][from.col() + dc / 2] = null;

        if (piece.getColor() == Color.RED   && to.row() == 0) piece.setKing(true);
        if (piece.getColor() == Color.BLACK && to.row() == 7) piece.setKing(true);
    }

    private static List<Move> getAllValidMoves(Piece[][] b, Color color) {
        List<Move> jumps = new ArrayList<>();
        List<Move> regular = new ArrayList<>();

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = b[r][c];
                if (p == null || p.getColor() != color) continue;
                Pos pos = new Pos(r, c);
                jumps.addAll(getJumps(b, pos, p));
                regular.addAll(getRegularMoves(b, pos, p));
            }
        }
        return jumps.isEmpty() ? regular : jumps;
    }

    private static List<Move> getRegularMoves(Piece[][] b, Pos from, Piece p) {
        List<Move> moves = new ArrayList<>();
        for (int[] d : directions(p)) {
            int nr = from.row() + d[0];
            int nc = from.col() + d[1];
            Pos to = new Pos(nr, nc);
            if (inBounds(to) && b[nr][nc] == null)
                moves.add(new Move(from, to));
        }
        return moves;
    }

    private static List<Move> getJumps(Piece[][] b, Pos from, Piece p) {
        List<Move> jumps = new ArrayList<>();
        for (int[] d : directions(p)) {
            int mr = from.row() + d[0],     mc = from.col() + d[1];
            int nr = from.row() + d[0] * 2, nc = from.col() + d[1] * 2;
            Pos to = new Pos(nr, nc);
            if (!inBounds(to)) continue;
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

    // ── Private — outcome detection ───────────────────────────────────────────

    private static String checkOutcome(Piece[][] b) {
        boolean hasRed = false, hasBlack = false;
        for (Piece[] row : b)
            for (Piece p : row)
                if (p != null) {
                    if (p.getColor() == Color.RED)   hasRed   = true;
                    if (p.getColor() == Color.BLACK) hasBlack = true;
                }

        if (!hasRed)   return Color.BLACK.getName();
        if (!hasBlack) return Color.RED.getName();

        boolean redCanMove   = !getAllValidMoves(b, Color.RED).isEmpty();
        boolean blackCanMove = !getAllValidMoves(b, Color.BLACK).isEmpty();

        if (!redCanMove && !blackCanMove) return "draw";
        if (!redCanMove)   return Color.BLACK.getName();
        if (!blackCanMove) return Color.RED.getName();
        return null;
    }

    // ── Private — board utilities ─────────────────────────────────────────────

    private static boolean inBounds(Pos p) {
        return p.row() >= 0 && p.row() < 8 && p.col() >= 0 && p.col() < 8;
    }

    private static Piece[][] copy(Piece[][] src) {
        Piece[][] copy = new Piece[8][8];
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                copy[r][c] = src[r][c] == null ? null : src[r][c].copy();
        return copy;
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

    // ── Private — AI (minimax with alpha-beta pruning) ────────────────────────

    private static MinimaxResult minimax(Piece[][] b, int depth,
                                         int alpha, int beta, boolean maximising) {
        Color      color = maximising ? AI_COLOR : PLAYER_COLOR;
        List<Move> moves = getAllValidMoves(b, color);

        if (depth == 0 || moves.isEmpty())
            return new MinimaxResult(null, evaluate(b));

        Move bestMove  = null;
        int  bestScore = maximising ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        for (Move m : moves) {
            Piece[][] copy  = copy(b);
            executeMove(copy, m.from(), m.to(), copy[m.from().row()][m.from().col()]);
            int score = minimax(copy, depth - 1, alpha, beta, !maximising).score;

            if (maximising) {
                if (score > bestScore) { bestScore = score; bestMove = m; }
                alpha = Math.max(alpha, bestScore);
            } else {
                if (score < bestScore) { bestScore = score; bestMove = m; }
                beta = Math.min(beta, bestScore);
            }
            if (beta <= alpha) break;
        }

        return new MinimaxResult(bestMove, bestScore);
    }

    private static int evaluate(Piece[][] b) {
        int score = 0;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = b[r][c];
                if (p == null) continue;
                int value = p.isKing() ? 3 : 1;
                if (!p.isKing())
                    value += p.getColor() == Color.BLACK ? (r / 2) : ((7 - r) / 2);
                score += p.getColor() == AI_COLOR ? value : -value;
            }
        }
        return score;
    }
}

package edu.uic.cs342.model;

import java.util.List;

public class AiPlayer implements Opponent {

    // ── Subrecords ────────────────────────────────────────────────────────────

    private record MinimaxResult(Board.Move move, int score) {}

    // ── Fields ────────────────────────────────────────────────────────────────

    private static final int      MAX_DEPTH    = 5;
    private static final Color    AI_COLOR     = Color.BLACK;
    private static final Color    PLAYER_COLOR = Color.RED;
    private static final AiPlayer INSTANCE     = new AiPlayer();

    // ── Constructors ──────────────────────────────────────────────────────────

    private AiPlayer() {}

    // ── Getters ───────────────────────────────────────────────────────────────

    public static AiPlayer getInstance() { return AiPlayer.INSTANCE; }

    @Override
    public String getName() { return "AI"; }

    // ── Methods ───────────────────────────────────────────────────────────────

    @Override
    public String applyMove(Board board, Color color, Color currentTurn,
                            Board.Pos from, Board.Pos to) {
        Board.Piece[][] copy = board.copyGrid();
        Board.Move best = AiPlayer.minimax(copy, AiPlayer.MAX_DEPTH,
                                           Integer.MIN_VALUE, Integer.MAX_VALUE, true).move;

        if (best == null) return null;

        return board.applyMove(best.from(), best.to(), color, currentTurn);
    }

    // ── Private — minimax with alpha-beta pruning ─────────────────────────────

    private static MinimaxResult minimax(Board.Piece[][] b, int depth,
                                         int alpha, int beta, boolean maximising) {
        Color            color = maximising ? AiPlayer.AI_COLOR : AiPlayer.PLAYER_COLOR;
        List<Board.Move> moves = Board.getAllValidMoves(b, color);

        if (depth == 0 || moves.isEmpty())
            return new MinimaxResult(null, AiPlayer.evaluate(b));

        Board.Move bestMove  = null;
        int        bestScore = maximising ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        for (Board.Move m : moves) {
            Board.Piece[][] copy  = Board.copyGrid(b);
            Board.executeMove(copy, m.from(), m.to(), copy[m.from().row()][m.from().col()]);
            int score = AiPlayer.minimax(copy, depth - 1, alpha, beta, !maximising).score;

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

    private static int evaluate(Board.Piece[][] b) {
        int score = 0;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Board.Piece p = b[r][c];
                if (p == null) continue;
                int value = p.isKing() ? 3 : 1;
                if (!p.isKing())
                    value += p.getColor() == Color.BLACK ? (r / 2) : ((7 - r) / 2);
                score += p.getColor() == AiPlayer.AI_COLOR ? value : -value;
            }
        }
        return score;
    }
}

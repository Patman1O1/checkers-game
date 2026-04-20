package edu.uic.cs342.model;

import java.util.List;

public class AiPlayer implements Opponent {
    // ── Subrecords ────────────────────────────────────────────────────────────
    private record MinimaxResult(Board.Move move, int score) {}

    // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────────
    private static final int MAX_DEPTH = 5;

    private static final Color AI_COLOR = Color.BLACK;

    private static final Color PLAYER_COLOR = Color.RED;

    private static final AiPlayer INSTANCE = new AiPlayer();

    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────
    private AiPlayer() {}

    // ── Getters ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public static AiPlayer getInstance() { return AiPlayer.INSTANCE; }

    @Override
    public String getName() { return "AI"; }

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    @Override
    public String applyMove(Board board, Color color, Color currentTurn, Board.Pos from, Board.Pos to) {
        Board.Piece[][] copy = board.copyGrid();
        Board.Move best = AiPlayer.minimax(copy, AiPlayer.MAX_DEPTH,
                                           Integer.MIN_VALUE, Integer.MAX_VALUE, true).move;

        if (best == null) {
            return null;
        }

        return board.applyMove(best.from(), best.to(), color, currentTurn);
    }

    private static MinimaxResult minimax(Board.Piece[][] board, int depth, int alpha, int beta, boolean maximising) {
        List<Board.Move> moves = Board.getAllValidMoves(board, maximising ? AiPlayer.AI_COLOR : AiPlayer.PLAYER_COLOR);

        if (depth == 0 || moves.isEmpty()) {
            return new MinimaxResult(null, AiPlayer.evaluate(board));
        }

        Board.Move bestMove = null;
        int bestScore = maximising ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        for (Board.Move move : moves) {
            Board.Piece[][] copy  = Board.copyGrid(board);
            Board.executeMove(copy, move.from(), move.to(), copy[move.from().row()][move.from().col()]);
            int score = AiPlayer.minimax(copy, depth - 1, alpha, beta, !maximising).score;

            if (maximising) {
                if (score > bestScore) {
                    bestScore = score;
                    bestMove = move;
                }
                alpha = Math.max(alpha, bestScore);
            } else {
                if (score < bestScore) {
                    bestScore = score;
                    bestMove = move;
                }
                beta = Math.min(beta, bestScore);
            }
            if (beta <= alpha) {
                break;
            }
        }

        return new MinimaxResult(bestMove, bestScore);
    }

    private static int evaluate(Board.Piece[][] board) {
        int score = 0;
        for (int rowNum = 0; rowNum < 8; ++rowNum) {
            for (int colNum = 0; colNum < 8; ++colNum) {
                Board.Piece piece = board[rowNum][colNum];
                if (piece == null) {
                    continue;
                }

                int value = piece.isKing() ? 3 : 1;
                if (!piece.isKing()) {
                    value += piece.getColor() == Color.BLACK ? (rowNum / 2) : ((7 - rowNum) / 2);
                }

                score += piece.getColor() == AiPlayer.AI_COLOR ? value : -value;
            }
        }
        return score;
    }
}

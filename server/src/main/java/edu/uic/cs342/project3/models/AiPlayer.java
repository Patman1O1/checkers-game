package edu.uic.cs342.project3.models;

import java.util.List;

public class AiPlayer implements Opponent {
    // ── MinimaxResult ────────────────────────────────────────────────────────────────────────────────────────────────
    private static class MinimaxResult {
        // The move to make (null at leaf nodes)
        private final Board.Move move;

        // The board evaluation score for this move
        private final int score;

        public MinimaxResult(Board.Move move, int score) {
            this.move = move;
            this.score = score;
        }
    }

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
    // Called by Router.makeMove() when it's the AI's turn; ignores the 'from' and 'to' parameters (calculates its own)
    @Override
    public String applyMove(Board board, Color color, Color currentTurn, Board.Pos from, Board.Pos to) {
        Board.Piece[][] copy = board.copyGrid();

        // Run minimax algorithm on the copy to find the best move; true = AI is the maximizing player at the root
        Board.Move best = AiPlayer.minimax(copy, AiPlayer.MAX_DEPTH,
                                           Integer.MIN_VALUE, Integer.MAX_VALUE, true).move;

        // If no moves are available...
        if (best == null) {
            return null; // (no error)
        }

        // Apply the best move
        return board.applyMove(best.from, best.to, color, currentTurn);
    }

    // Recursively finds the best move by exploring the game tree up to 'depth' levels deep
    // alpha = the best score the maximizer (AI) is guaranteed so far
    // beta  = the best score the minimizer (human) is guaranteed so far
    // maximizing = true when it's AI's turn, false when it's the human's turn
    private static MinimaxResult minimax(Board.Piece[][] board, int depth, int alpha, int beta, boolean maximising) {
        // Get all valid moves for whichever player's turn it is in this simulated position
        List<Board.Move> moves = Board.getAllValidMoves(board, maximising ? AiPlayer.AI_COLOR : AiPlayer.PLAYER_COLOR);

        // Base cases: either we've hit the search depth limit, or there are no moves (game over)
        if (depth == 0 || moves.isEmpty()) {
            return new MinimaxResult(null, AiPlayer.evaluate(board));
        }

        Board.Move bestMove = null;
        int bestScore = maximising ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        for (Board.Move move : moves) {
            // Copy the board for the current move
            Board.Piece[][] copy = Board.copyGrid(board);

            // Simulate the move on the copy
            Board.executeMove(copy, move.from, move.to, copy[move.from.rowNum][move.from.colNum]);

            // Recursively evaluate the resulting position from the opponent's perspective
            int score = AiPlayer.minimax(copy, depth - 1, alpha, beta, !maximising).score;

            if (maximising) { // If the AI wants the highest possible score...
                if (score > bestScore) {
                    bestScore = score;
                    bestMove = move;
                }
                alpha = Math.max(alpha, bestScore);
            } else { // Otherwise, if the human wants the lowest possible score (for the AI)
                if (score < bestScore) {
                    bestScore = score;
                    bestMove = move;
                }
                // Update beta (best guaranteed score for human)
                beta = Math.min(beta, bestScore);
            }

            // Alpha-beta pruning: if beta <= alpha, the opponent won't allow this branch to happen
            // because they already have a better option elsewhere — no need to explore further
            if (beta <= alpha) {
                break;
            }
        }

        return new MinimaxResult(bestMove, bestScore);
    }

    // Returns a score representing how good the position is for the AI (positive = AI advantage)
    private static int evaluate(Board.Piece[][] board) {
        int score = 0;

        // For each row...
        for (int rowNum = 0; rowNum < 8; ++rowNum) {
            // For each column...
            for (int colNum = 0; colNum < 8; ++colNum) {
                // Get the peice at the current row and column
                Board.Piece piece = board[rowNum][colNum];

                // If there is no piece at the square...
                if (piece == null) {
                    // Continue to the next row, column pair
                    continue;
                }

                // Kings are worth 3 times the value of a regular piece
                int value = piece.isKing() ? 3 : 1;

                // If the piece is a regular piece
                if (!piece.isKing()) {
                    // Black moves DOWN (increasing row), so higher row = closer to row 7 (promotion)
                    // Red moves UP (decreasing row), so lower row = closer to row 0 (promotion)
                    value += piece.getColor() == Color.BLACK ? (rowNum / 2) : ((7 - rowNum) / 2);
                }

                // Add for AI pieces, subtract for player pieces
                // This means a high score = AI is winning, low score = player is winning
                score += piece.getColor() == AiPlayer.AI_COLOR ? value : -value;
            }
        }
        return score;
    }
}

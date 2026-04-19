package edu.uic.cs342.model;

/**
 * Represents a participant in a checkers game — either a human Player or the AI.
 *
 * Implementations are responsible for validating and executing their move
 * on the provided Board for the given Color.
 *
 * @see Player
 * @see edu.uic.cs342.model.AiPlayer
 */
public interface Opponent {

    /**
     * Attempt to apply this opponent's move to the board.
     *
     * @param board       the live board to apply the move to
     * @param color       the color this opponent is playing as
     * @param currentTurn the color whose turn it currently is
     * @param from        the source position (ignored by AiPlayer — it picks its own)
     * @param to          the destination position (ignored by AiPlayer)
     * @return null on success, or a non-null error string on failure
     */
    String applyMove(Board board, Color color, Color currentTurn, Board.Pos from, Board.Pos to);

    /**
     * Returns the name that identifies this opponent in the game
     * (the username for a human Player, "AI" for the AI).
     */
    String getName();
}

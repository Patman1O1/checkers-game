package edu.uic.cs342.project3.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a checkers game between two players (or a player vs the AI).
 *
 * Owns game identity, player names, turn state, status, and the chat log.
 * All board logic (piece movement, move validation, win detection, AI) is
 * delegated to the {@link Board} field.
 */
public class CheckersGame {

    // ── Subenums / Subclasses ─────────────────────────────────────────────────

    public enum Status { WAITING, ACTIVE, COMPLETED }

    /** An in-game chat message. */
    public static class ChatEntry {

        // ── Fields ────────────────────────────────────────────────────────────

        @JsonProperty("player")    public String player;
        @JsonProperty("message")   public String message;
        @JsonProperty("timestamp") public String timestamp;

        // ── Constructors ──────────────────────────────────────────────────────

        public ChatEntry() {}

        public ChatEntry(String player, String message) {
            this.player    = player;
            this.message   = message;
            this.timestamp = Instant.now().toString();
        }
    }

    // ── Fields ────────────────────────────────────────────────────────────────

    @JsonProperty("id")          private final String          id;
    @JsonProperty("player1")     private final String          player1;
    @JsonProperty("player2")     private       String          player2;
    @JsonProperty("vsAI")        private final boolean         vsAI;
    @JsonProperty("board")       private final Board           board;
    @JsonProperty("currentTurn") private       Color           currentTurn;
    @JsonProperty("status")      private       Status          status;
    @JsonProperty("winner")      private       String          winner;
    @JsonProperty("chat")        private       List<ChatEntry> chat;

    // ── Constructors ──────────────────────────────────────────────────────────

    public CheckersGame(String player1, String player2, boolean vsAI) {
        this.id          = UUID.randomUUID().toString();
        this.player1     = player1;
        this.player2     = vsAI ? "AI" : player2;
        this.vsAI        = vsAI;
        this.board       = new Board();
        this.currentTurn = Color.RED;
        this.status      = Status.ACTIVE;
        this.winner      = null;
        this.chat        = new ArrayList<>();
    }

    /** No-arg constructor for Jackson. */
    public CheckersGame() {
        this.id      = UUID.randomUUID().toString();
        this.player1 = "";
        this.vsAI    = false;
        this.board   = new Board();
    }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setPlayer2(String p)      { this.player2     = p; }
    public void setCurrentTurn(Color t)   { this.currentTurn = t; }
    public void setStatus(Status s)       { this.status      = s; }
    public void setWinner(String w)       { this.winner      = w; }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String          getId()          { return id;          }
    public String          getPlayer1()     { return player1;     }
    public String          getPlayer2()     { return player2;     }
    public boolean         isVsAI()         { return vsAI;        }
    public Board           getBoard()       { return board;       }
    public Color           getCurrentTurn() { return currentTurn; }
    public Status          getStatus()      { return status;      }
    public String          getWinner()      { return winner;      }
    public List<ChatEntry> getChat()        { return chat;        }

    // ── Methods ───────────────────────────────────────────────────────────────

    public void addChat(String player, String message) {
        chat.add(new ChatEntry(player, message));
    }

    public void flipTurn() {
        currentTurn = currentTurn.opposite();
    }

    public void endGame(String outcome) {
        this.winner = outcome;
        this.status = Status.COMPLETED;
    }

    public boolean hasPlayer(String username) {
        return player1.equalsIgnoreCase(username) || player2.equalsIgnoreCase(username);
    }

    /** Returns the Color assigned to the given username. */
    public Color colorOf(String username) {
        return player1.equalsIgnoreCase(username) ? Color.RED : Color.BLACK;
    }

    /**
     * Validate and apply a player's move, then check for a win or draw.
     * @return null on success, or a non-null error string on failure
     */
    public String applyMove(Board.Pos from, Board.Pos to, String playerUsername) {
        Color  playerColor = colorOf(playerUsername);
        String error       = board.applyMove(from, to, playerColor, currentTurn);
        if (error != null) return error;

        String outcome = board.checkOutcome();
        if (outcome != null) endGame(outcome);
        else                 flipTurn();

        return null;
    }

    /**
     * Compute and apply the best AI move.
     * @return null on success, or a non-null error string on failure
     */
    public String applyAiMove() {
        Board.Move best = board.bestAiMove();

        if (best == null) {
            endGame(Color.RED.getValue());
            return null;
        }

        return applyMove(best.from(), best.to(), "AI");
    }
}

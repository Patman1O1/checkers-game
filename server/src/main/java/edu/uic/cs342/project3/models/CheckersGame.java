package edu.uic.cs342.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CheckersGame {

    // ── Subenums / Subclasses ─────────────────────────────────────────────────

    public enum Status { WAITING, ACTIVE, COMPLETED }

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
        this.status      = CheckersGame.Status.ACTIVE;
        this.winner      = null;
        this.chat        = new ArrayList<>();
    }

    public CheckersGame() {
        this.id      = UUID.randomUUID().toString();
        this.player1 = "";
        this.vsAI    = false;
        this.board   = new Board();
    }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setPlayer2(String p)    { this.player2     = p; }
    public void setCurrentTurn(Color t) { this.currentTurn = t; }
    public void setStatus(Status s)     { this.status      = s; }
    public void setWinner(String w)     { this.winner      = w; }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String          getId()          { return this.id;          }
    public String          getPlayer1()     { return this.player1;     }
    public String          getPlayer2()     { return this.player2;     }
    public boolean         isVsAI()         { return this.vsAI;        }
    public Board           getBoard()       { return this.board;       }
    public Color           getCurrentTurn() { return this.currentTurn; }
    public Status          getStatus()      { return this.status;      }
    public String          getWinner()      { return this.winner;      }
    public List<ChatEntry> getChat()        { return this.chat;        }

    // ── Methods ───────────────────────────────────────────────────────────────

    public void addChat(String player, String message) {
        this.chat.add(new CheckersGame.ChatEntry(player, message));
    }

    public void flipTurn() {
        this.currentTurn = this.currentTurn.opposite();
    }

    public void endGame(String outcome) {
        this.winner = outcome;
        this.status = CheckersGame.Status.COMPLETED;
    }

    public boolean hasPlayer(String username) {
        return this.player1.equalsIgnoreCase(username) || this.player2.equalsIgnoreCase(username);
    }

    public Color colorOf(String username) {
        return this.player1.equalsIgnoreCase(username) ? Color.RED : Color.BLACK;
    }

    public String takeTurn(Opponent opponent, Board.Pos from, Board.Pos to) {
        Color playerColor = this.colorOf(opponent.getName());

        if (opponent instanceof AiPlayer && this.board.validMoves(Color.BLACK).isEmpty()) {
            this.endGame(Color.RED.getValue());
            return null;
        }

        String error = opponent.applyMove(this.board, playerColor, this.currentTurn, from, to);
        if (error != null) return error;

        String outcome = this.board.checkOutcome();
        if (outcome != null) this.endGame(outcome);
        else                 this.flipTurn();

        return null;
    }
}

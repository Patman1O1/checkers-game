package edu.uic.cs342.project3.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Client-side representation of game state received from the server.
 * Maps 1-to-1 with CheckersGame serialised JSON.
 *
 * Color constants RED and BLACK are provided as static finals so
 * controllers can compare against them without raw string literals.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameState {

    // ── Subclasses ────────────────────────────────────────────────────────────

    /** Mirrors Board.Piece — color (as lowercase string) and king flag. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Piece {
        @JsonProperty("color") public String  color;
        @JsonProperty("king")  public boolean king;
    }

    /** Mirrors Board serialised form — wraps the 8×8 grid. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Board {
        @JsonProperty("grid") public Piece[][] grid;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChatEntry {
        @JsonProperty("player")    public String player;
        @JsonProperty("message")   public String message;
        @JsonProperty("timestamp") public String timestamp;
    }

    // ── Fields ────────────────────────────────────────────────────────────────

    /** Color string constant matching server-side Color.RED.getValue(). */
    public static final String RED   = "red";

    /** Color string constant matching server-side Color.BLACK.getValue(). */
    public static final String BLACK = "black";

    @JsonProperty("id")          public String          id;
    @JsonProperty("player1")     public String          player1;
    @JsonProperty("player2")     public String          player2;
    @JsonProperty("vsAI")        public boolean         vsAI;
    @JsonProperty("board")       public Board           board;
    @JsonProperty("currentTurn") public String          currentTurn;
    @JsonProperty("status")      public String          status;
    @JsonProperty("winner")      public String          winner;
    @JsonProperty("chat")        public List<ChatEntry> chat;

    // ── Methods ───────────────────────────────────────────────────────────────

    public boolean isCompleted() { return "COMPLETED".equals(status); }
    public boolean isActive()    { return "ACTIVE".equals(status);    }
}

package edu.uic.cs342.project3.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GameState {

    // ── Subclasses ────────────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Piece {
        @JsonProperty("color") public String  color;
        @JsonProperty("king")  public boolean king;
    }

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

    public static final String RED   = "red";
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

    public boolean isCompleted() { return "COMPLETED".equals(this.status); }
    public boolean isActive()    { return "ACTIVE".equals(this.status);    }
}

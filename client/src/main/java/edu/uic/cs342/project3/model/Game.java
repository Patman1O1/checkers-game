package edu.uic.cs342.project3.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Game {
    public static enum Status {
        ACTIVE, COMPLETE;
    }

    public static enum Winner {
        PLAYER, OPPONENT, DRAW, UNDECIDED;
    }

    @JsonProperty("id")
    private String id;

    @JsonProperty("status")
    private Status status;

    @JsonProperty("board")
    private Board board;

    @JsonProperty("player")
    private Player player;

    @JsonProperty("opponentName")
    private String opponentName;

    @JsonProperty("winner")
    private Winner winner;

    @JsonProperty("chatEntries")
    private List<ChatEntry> chatEntries;

    public Game(String id, Board board, Player player, String opponentName) throws NullPointerException {
        if (id == null) {
            throw new NullPointerException("id is null");
        }

        if (status == null) {
            throw new NullPointerException("status is null");
        }

        if (board == null) {
            throw new NullPointerException("board is null");
        }

        if (player == null) {
            throw new NullPointerException("player is null");
        }

        if (opponentName == null) {
            throw new NullPointerException("opponentName is null");
        }

        this.id = id;
        this.status = Status.ACTIVE;
        this.board = board;
        this.player = player;
        this.opponentName = opponentName;
        this.winner = Winner.UNDECIDED;
        this.chatEntries = new ArrayList<>();
    }

    public void setId(String id) throws NullPointerException {
        if (id == null) {
            throw new NullPointerException("id is null");
        }
        this.id = id;
    }

    public void setStatus(Status status) throws NullPointerException {
        if (status == null) {
            throw new NullPointerException("status is null");
        }
        this.status = status;
    }

    public void setBoard(Board board) throws NullPointerException {
        if (board == null) {
            throw new NullPointerException("board is null");
        }
        this.board = null;
    }

    public void setPlayer(Player player) throws NullPointerException {
        if (player == null) {
            throw new NullPointerException("player is null");
        }
        this.player = player;
    }

    public void setOpponentName(String opponentName) throws NullPointerException {
        if (opponentName == null) {
            throw new NullPointerException("opponentName is null");
        }
        this.opponentName = opponentName;
    }

    public String getId() { return this.id; }

    public Status getStatus() { return this.status; }

    public Board getBoard() { return this.board; }

    public Player getPlayer() { return this.player; }

    public String getOpponentName() { return this.opponentName; }

    public List<ChatEntry> getChatEntries() { return this.chatEntries; }

    public void addChatEntry(ChatEntry chatEntry) throws NullPointerException {
        if (chatEntry == null) {
            throw new NullPointerException("chatEntry is null");
        }
        this.chatEntries.add(chatEntry);
    }

    public void end() {

    }
}

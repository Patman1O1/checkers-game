package edu.uic.cs342.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Player implements Opponent {

    // ── Subclasses / Subenums ─────────────────────────────────────────────────

    public enum Status { ONLINE, OFFLINE }

    // ── Fields ────────────────────────────────────────────────────────────────

    @JsonProperty("username")               private String       username;
    @JsonProperty("password")               private String       password;
    @JsonProperty("wins")                   private int          wins;
    @JsonProperty("losses")                 private int          losses;
    @JsonProperty("draws")                  private int          draws;
    @JsonProperty("friends")               private List<String> friends;
    @JsonProperty("incomingFriendRequests") private List<String> incomingFriendRequests;
    @JsonProperty("outgoingFriendRequests") private List<String> outgoingFriendRequests;

    private transient Status status = Player.Status.OFFLINE;

    // ── Constructors ──────────────────────────────────────────────────────────

    public Player() {
        this.friends                = new ArrayList<>();
        this.incomingFriendRequests = new ArrayList<>();
        this.outgoingFriendRequests = new ArrayList<>();
    }

    public Player(String username, String password) {
        this.username               = username;
        this.password               = password;
        this.wins                   = 0;
        this.losses                 = 0;
        this.draws                  = 0;
        this.friends                = new ArrayList<>();
        this.incomingFriendRequests = new ArrayList<>();
        this.outgoingFriendRequests = new ArrayList<>();
        this.status                 = Player.Status.OFFLINE;
    }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setStatus(Status s) { this.status = s; }
    public void setWins(int w)      { this.wins   = w; }
    public void setLosses(int l)    { this.losses = l; }
    public void setDraws(int d)     { this.draws  = d; }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String       getUsername()               { return this.username;               }
    public String       getPassword()               { return this.password;               }
    public int          getWins()                   { return this.wins;                   }
    public int          getLosses()                 { return this.losses;                 }
    public int          getDraws()                  { return this.draws;                  }
    public List<String> getFriends()               { return this.friends;               }
    public List<String> getIncomingFriendRequests() { return this.incomingFriendRequests; }
    public List<String> getOutgoingFriendRequests() { return this.outgoingFriendRequests; }
    public Status       getStatus()                { return this.status;                }

    // ── Methods ───────────────────────────────────────────────────────────────

    @Override
    public String applyMove(Board board, Color color, Color currentTurn,
                            Board.Pos from, Board.Pos to) {
        return board.applyMove(from, to, color, currentTurn);
    }

    @Override
    public String getName() { return this.username; }

    public void addWin()  { this.wins++;   }
    public void addLoss() { this.losses++; }
    public void addDraw() { this.draws++;  }

    public void addFriend(String friend) {
        if (!this.friends.contains(friend)) this.friends.add(friend);
    }

    public void addIncomingRequest(String from) {
        if (!this.incomingFriendRequests.contains(from)) this.incomingFriendRequests.add(from);
    }

    public void addOutgoingRequest(String to) {
        if (!this.outgoingFriendRequests.contains(to)) this.outgoingFriendRequests.add(to);
    }

    public void removeIncomingRequest(String from) { this.incomingFriendRequests.remove(from); }
    public void removeOutgoingRequest(String to)   { this.outgoingFriendRequests.remove(to);   }

    public boolean isFriend(String other) {
        return this.friends.stream().anyMatch(f -> f.equalsIgnoreCase(other));
    }

    public boolean hasPendingRequestFrom(String other) {
        return this.incomingFriendRequests.stream().anyMatch(f -> f.equalsIgnoreCase(other));
    }

    public boolean hasSentRequestTo(String other) {
        return this.outgoingFriendRequests.stream().anyMatch(f -> f.equalsIgnoreCase(other));
    }

    public boolean checkPassword(String pw) {
        return this.password != null && this.password.equals(pw);
    }

    @Override
    public String toString() { return this.username; }
}

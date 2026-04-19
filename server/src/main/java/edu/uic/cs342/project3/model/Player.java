package edu.uic.cs342.project3.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import edu.uic.cs342.project3.json.deserializers.PlayerDeserializer;
import edu.uic.cs342.project3.json.serializers.PlayerSerializer;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(using = PlayerSerializer.class)
@JsonDeserialize(using = PlayerDeserializer.class)
public class Player {
    // ── Status ───────────────────────────────────────────────────────────────────────────────────────────────────────
    public static enum Status {
        // ── Constants ────────────────────────────────────────────────────────────────────────────────────────────────
        ONLINE, OFFLINE;
    }

    // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────────
    @JsonProperty("username")
    private String username;

    @JsonProperty("password")
    private String password;

    @JsonProperty("wins")
    private int wins;

    @JsonProperty("losses")
    private int losses;

    @JsonProperty("draws")
    private int draws;

    @JsonProperty("friends")
    private List<String> friends;

    private transient Status status = Status.OFFLINE;

    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────

    public Player() {
        this.friends = new ArrayList<>();
    }

    public Player(String username, String password) {
        this.username = username;
        this.password = password;
        this.wins     = 0;
        this.losses   = 0;
        this.draws    = 0;
        this.friends  = new ArrayList<>();
        this.status   = Status.OFFLINE;
    }

    // ── Setters ──────────────────────────────────────────────────────────────────────────────────────────────────────

    public void setStatus(Status s)  { this.status = s; }
    public void setWins(int w)       { this.wins   = w; }
    public void setLosses(int l)     { this.losses = l; }
    public void setDraws(int d)      { this.draws  = d; }

    // ── Getters ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public String       getUsername() { return username; }
    public String       getPassword() { return password; }
    public int          getWins()     { return wins;     }
    public int          getLosses()   { return losses;   }
    public int          getDraws()    { return draws;    }
    public List<String> getFriends()  { return friends;  }
    public Status       getStatus()   { return status;   }

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────

    public void addWin()  { wins++;   }
    public void addLoss() { losses++; }
    public void addDraw() { draws++;  }

    public void addFriend(String friend) {
        if (!friends.contains(friend)) friends.add(friend);
    }

    public boolean checkPassword(String pw) {
        return this.password != null && this.password.equals(pw);
    }

    @Override
    public String toString() { return username; }
}

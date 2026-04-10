package edu.uic.cs342.project3.backend;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = PlayerDeserializer.class)
@JsonSerialize(using = PlayerSerializer.class)
public class Player implements Serializable {
    // ── Status ───────────────────────────────────────────────────────────────────────────────────────────────────────
    public static enum Status implements Serializable {
        // ── Enum Constants ───────────────────────────────────────────────────────────────────────────────────────────
        ONLINE,
        OFFLINE
    }

    // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────────
    private static final long serialVersionUID = 42L;

    private static final Logger LOGGER = Logger.getLogger(Player.class.getName());

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

    private Status status;

    @JsonProperty("friends")
    private List<String> friends;

    // ── Constructors ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public Player() {
        this.username = "";
        this.password = "";
        this.wins = this.losses = this.draws = 0;
        this.status = Status.OFFLINE;
        this.friends = new ArrayList<>();
    }

    public Player(String username, String password) throws NullPointerException {
        if (username == null) {
            throw new NullPointerException("username is null");
        }

        if (password == null) {
            throw new NullPointerException("password is null");
        }

        this.username = username;
        this.password = password;
        this.wins = this.losses = this.draws = 0;
        this.friends = new ArrayList<>();
    }

    public Player(String username,
                  String password,
                  int wins,
                  int losses,
                  int draws,
                  Status status,
                  List<String> friends) throws NullPointerException {
        if (username == null) {
            throw new NullPointerException("username is null");
        }

        if (password == null) {
            throw new NullPointerException("password is null");
        }

        if (status == null) {
            throw new NullPointerException("status is null");
        }

        if (friends == null) {
            throw new NullPointerException("friends is null");
        }

        this.username = username;
        this.password = password;
        this.wins = wins;
        this.losses = losses;
        this.draws = draws;
        this.status = status;
        this.friends = friends;
    }

    // ── Setters ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public void setStatus(Status status) throws NullPointerException {
        if (status == null) {
            throw new NullPointerException("status is null");
        }
        this.status = status;
    }

    // ── Getters ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public String getUsername() { return this.username; }

    public String getPassword() { return this.password; }

    public int getWins() { return this.wins; }

    public int getLosses() { return this.losses; }

    public int getDraws() { return this.draws; }

    public List<String> getFriends() { return this.friends; }

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    @Override
    public String toString() { return this.username; }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Player)) {
            return false;
        }

        if (this == object) {
            return true;
        }

        Player other = (Player) object;
        return this.username.equals(other.username) &&
                this.password.equals(other.username) &&
                this.wins == other.wins &&
                this.losses == other.losses &&
                this.draws == other.draws &&
                this.status.equals(other.status) &&
                this.friends.equals(other.friends);
    }

    public void addFriend(String friend) throws NullPointerException {
        if (friend == null) {
            throw new NullPointerException("friend is null");
        }
        this.friends.add(friend);
    }

    public void addFriends(List<String> friends) throws NullPointerException {
        if (friends == null) {
            throw new NullPointerException("friends is null");
        }
        this.friends.addAll(friends);
    }

    public void removeFriend(String friendName) throws NullPointerException {
        if (friendName == null) {
            throw new NullPointerException("friend is null");
        }

        this.friends.remove(friendName);
    }

    public void removeFriends(List<String> friends) throws NullPointerException {
        if (friends == null) {
            throw new RuntimeException("friends is null");
        }

        this.friends.removeAll(friends);
    }

    public void addWin() { ++this.wins; }

    public void addWins(int wins) { this.wins += wins; }

    public void addLoss() { ++this.losses; }

    public void addLosses(int losses) { this.losses += losses; }

    public void addDraw() { ++this.draws; }

    public void addDraws(int draws) { this.draws += draws; }

    public void save(File playerFile) {

    }
}

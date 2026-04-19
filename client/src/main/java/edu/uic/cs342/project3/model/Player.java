package edu.uic.cs342.project3.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.uic.cs342.project3.http.Callback;
import edu.uic.cs342.project3.http.ClientThread;
import edu.uic.cs342.project3.http.HttpMethod;
import edu.uic.cs342.project3.http.HttpRequest;
import edu.uic.cs342.project3.json.deserializers.PlayerDeserializer;
import edu.uic.cs342.project3.json.serializers.PlayerSerializer;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(using = PlayerSerializer.class)
@JsonDeserialize(using = PlayerDeserializer.class)
public class Player {
    // ── Status ───────────────────────────────────────────────────────────────────────────────────────────────────────
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static enum Status {
        // ── Constants ────────────────────────────────────────────────────────────────────────────────────────────────
        ONLINE("online"), OFFLINE("offline");

        // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────
        private final String value;

        // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────
        private Status(String value) { this.value = value; }

        // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────
        @Override
        public String toString() { return this.value; }
    }

    // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────────
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private transient final ClientThread clientThread;

    @JsonProperty("username")
    private String username;

    @JsonProperty("password")
    private String password;

    @JsonProperty("color")
    private Color color;

    @JsonProperty("wins")
    private int wins;

    @JsonProperty("losses")
    private int losses;

    @JsonProperty("draws")
    private int draws;

    @JsonProperty("status")
    private Status status;

    @JsonProperty("friends")
    private List<String> friends;

    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────
    public Player(String username, String password) throws NullPointerException {
        if (username == null) {
            throw new NullPointerException("username is null");
        }

        if (password == null) {
            throw new NullPointerException("password is null");
        }
        this.clientThread = ClientThread.getInstance();
        this.username = username;
        this.password = password;
        this.color = null;
        this.wins = this.losses = this.draws = 0;
        this.status = Status.OFFLINE;
        this.friends = new ArrayList<>();
    }

    public Player(String username, String password, Color color, int wins, int losses, int draws, Status status, List<String> friends)
            throws NullPointerException {
        if (username == null) {
            throw new NullPointerException("username is null");
        }

        if (password == null) {
            throw new NullPointerException("password is null");
        }

        this.clientThread = ClientThread.getInstance();
        this.username = username;
        this.password = password;
        this.color = color;
        this.wins = wins;
        this.losses = losses;
        this.draws = draws;
        this.status = status != null ? status : Status.OFFLINE;
        this.friends = new ArrayList<>(friends);
    }

    // ── Setters ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public void setUsername(String username) throws NullPointerException {
        if (username == null) {
            throw new NullPointerException("username is null");
        }
        this.username = username;
    }

    public void setPassword(String password) throws NullPointerException {
        if (password == null) {
            throw new NullPointerException("password is null");
        }
        this.password = password;
    }

    public void setColor(Color color) { this.color = color; }

    // ── Getters ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public String getUsername() { return this.username; }

    public String getPassword() { return this.password; }

    public Color getColor() { return this.color; }

    public int getWins() { return this.wins; }

    public int getLosses() { return this.losses; }

    public int getDraws() { return this.draws; }

    public Status getStatus() { return this.status; }

    public List<String> getFriends() { return this.friends; }

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    @Override
    public String toString() { return this.username; }

    public void signup(Callback callback) throws UnknownHostException {
        ObjectNode body = Player.OBJECT_MAPPER.createObjectNode();
        body.put("username", this.username);
        body.put("password", this.password);
        this.clientThread.sendRequest(new HttpRequest(HttpMethod.POST, "/auth/signup", body.toString()), callback);
    }

    public void login(Callback callback) throws UnknownHostException {
        ObjectNode body = Player.OBJECT_MAPPER.createObjectNode();
        body.put("username", this.username);
        body.put("password", this.password);
        this.clientThread.sendRequest(new HttpRequest(HttpMethod.POST, "/auth/login", body.toString()), callback);
    }

    public void logout(Callback callback) throws UnknownHostException {
        ObjectNode body = Player.OBJECT_MAPPER.createObjectNode();
        body.put("username", this.username);
        this.clientThread.sendRequest(new HttpRequest(HttpMethod.POST, "/auth/logout", body.toString()), callback);
    }

    public void addWin() { ++this.wins; }

    public void addLoss() { ++this.losses; }

    public void addDraw() { ++this.draws; }
}

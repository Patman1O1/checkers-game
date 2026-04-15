package edu.uic.cs342.project3.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.application.Platform;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Modelled after ClientThread from the HW5 starter code.
 * Opens one persistent connection to the server and keeps it open for the
 * lifetime of the application.
 *
 * Domain-specific methods (login, signup, createGame, etc.) build the request
 * body and delegate to get() / post(), so controllers interact with this class
 * directly without an intermediate ApiService layer.
 */
public class ClientThread extends Thread {

    // ── Subrecords ────────────────────────────────────────────────────────────

    private record Callbacks(Consumer<JsonNode> onSuccess, Consumer<String> onError) {}

    // ── Fields ────────────────────────────────────────────────────────────────

    private static final Logger       LOG      = Logger.getLogger(ClientThread.class.getName());
    private static final String       HOST     = "localhost";
    private static final int          PORT     = 8080;
    private static final ObjectMapper MAPPER   = new ObjectMapper();
    private static final ClientThread INSTANCE = new ClientThread();

    private Socket             socket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream  inputStream;

    private final AtomicLong           sequenceCounter = new AtomicLong(0);
    private final Map<Long, Callbacks> pending         = new ConcurrentHashMap<>();

    // ── Constructors ──────────────────────────────────────────────────────────

    private ClientThread() {
        setDaemon(true);
        setName("http-client");
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public static ClientThread getInstance() { return INSTANCE; }

    // ── Methods ───────────────────────────────────────────────────────────────

    // ── Auth ──────────────────────────────────────────────────────────────────

    public void signup(String username, String password,
                       Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("username", username);
        body.put("password", password);
        post("/auth/signup", body.toString(), onSuccess, onError);
    }

    public void login(String username, String password,
                      Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("username", username);
        body.put("password", password);
        post("/auth/login", body.toString(), onSuccess, onError);
    }

    public void logout(String username,
                       Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("username", username);
        post("/auth/logout", body.toString(), onSuccess, onError);
    }

    // ── Users ─────────────────────────────────────────────────────────────────

    public void getOnlineUsers(Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        get("/users/online", onSuccess, onError);
    }

    public void getUserStats(String username,
                             Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        get("/users/" + username + "/stats", onSuccess, onError);
    }

    // ── Friends ───────────────────────────────────────────────────────────────

    public void addFriend(String username, String friendUsername,
                          Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("username",       username);
        body.put("friendUsername", friendUsername);
        post("/friends/add", body.toString(), onSuccess, onError);
    }

    public void getFriends(String username,
                           Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        get("/friends/" + username, onSuccess, onError);
    }

    // ── Games ─────────────────────────────────────────────────────────────────

    public void createGame(String player1, String player2, boolean vsAI,
                           Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("player1", player1);
        body.put("vsAI",    vsAI);
        if (player2 != null && !player2.isBlank()) body.put("player2", player2);
        post("/game/create", body.toString(), onSuccess, onError);
    }

    public void getGameState(String gameId,
                             Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        get("/game/" + gameId, onSuccess, onError);
    }

    public void makeMove(String gameId,
                         int fromRow, int fromCol, int toRow, int toCol,
                         String player,
                         Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("player", player);
        ObjectNode from = MAPPER.createObjectNode();
        from.put("row", fromRow);
        from.put("col", fromCol);
        ObjectNode to = MAPPER.createObjectNode();
        to.put("row", toRow);
        to.put("col", toCol);
        body.set("from", from);
        body.set("to",   to);
        post("/game/" + gameId + "/move", body.toString(), onSuccess, onError);
    }

    public void sendChatMessage(String gameId, String player, String message,
                                Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("player",  player);
        body.put("message", message);
        post("/game/" + gameId + "/chat", body.toString(), onSuccess, onError);
    }

    public void endGame(String gameId, String winner,
                        Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("winner", winner);
        post("/game/" + gameId + "/end", body.toString(), onSuccess, onError);
    }

    // ── Connection lifecycle ──────────────────────────────────────────────────

    public void close() {
        try {
            if (this.inputStream  != null) this.inputStream.close();
            if (this.outputStream != null) this.outputStream.close();
            if (this.socket       != null) this.socket.close();
        } catch (IOException ignored) {}
    }

    @Override
    public void run() {
        try {
            this.socket = new Socket(InetAddress.getLocalHost(), PORT);
            this.outputStream = new ObjectOutputStream(this.socket.getOutputStream());
            this.outputStream.flush();
            this.inputStream  = new ObjectInputStream(this.socket.getInputStream());
            this.socket.setTcpNoDelay(true);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Could not connect to server: " + e.getMessage(), e);
            failAllPending("Could not connect to server: " + e.getMessage());
            return;
        }

        while (!Thread.currentThread().isInterrupted()) {
            try {
                HttpResponse res = (HttpResponse) this.inputStream.readObject();
                dispatch(res);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Connection to server lost: " + e.getMessage(), e);
                failAllPending("Connection to server lost.");
                break;
            }
        }

        close();
    }

    // ── Transport ─────────────────────────────────────────────────────────────

    private void get(String path, Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        send(new HttpRequest(sequenceCounter.incrementAndGet(), "GET", path), onSuccess, onError);
    }

    private void post(String path, String jsonBody,
                      Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        send(new HttpRequest(sequenceCounter.incrementAndGet(), "POST", path, jsonBody),
             onSuccess, onError);
    }

    private synchronized void send(HttpRequest req,
                                   Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        if (this.outputStream == null) {
            Platform.runLater(() -> onError.accept("Not connected to server."));
            return;
        }

        pending.put(req.getSequenceId(), new Callbacks(onSuccess, onError));

        try {
            this.outputStream.writeObject(req);
            this.outputStream.flush();
        } catch (IOException e) {
            pending.remove(req.getSequenceId());
            Platform.runLater(() -> onError.accept("Failed to send request: " + e.getMessage()));
            LOG.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private void dispatch(HttpResponse res) {
        Callbacks cb = pending.remove(res.getSequenceId());
        if (cb == null) {
            LOG.warning("No callback for sequenceId " + res.getSequenceId());
            return;
        }

        String body = res.getBody();
        try {
            JsonNode json = (body == null || body.isBlank())
                    ? MAPPER.createObjectNode()
                    : MAPPER.readTree(body);

            if (res.isOk()) {
                Platform.runLater(() -> cb.onSuccess.accept(json));
            } else {
                String error = json.has("error")
                        ? json.get("error").asText()
                        : "Server error " + res.getStatusCode();
                Platform.runLater(() -> cb.onError.accept(error));
            }
        } catch (Exception e) {
            Platform.runLater(() -> cb.onError.accept("Failed to parse response: " + e.getMessage()));
        }
    }

    private void failAllPending(String reason) {
        pending.forEach((id, cb) -> Platform.runLater(() -> cb.onError.accept(reason)));
        pending.clear();
    }
}

package edu.uic.cs342.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.application.Platform;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    private Callbacks pending;

    // ── Constructors ──────────────────────────────────────────────────────────

    private ClientThread() {
        this.setDaemon(true);
        this.setName("http-client");
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public static ClientThread getInstance() { return ClientThread.INSTANCE; }

    // ── Auth ──────────────────────────────────────────────────────────────────

    public void signup(String username, String password,
                       Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        ObjectNode body = ClientThread.MAPPER.createObjectNode();
        body.put("username", username);
        body.put("password", password);
        this.post("/auth/signup", body.toString(), onSuccess, onError);
    }

    public void login(String username, String password,
                      Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        ObjectNode body = ClientThread.MAPPER.createObjectNode();
        body.put("username", username);
        body.put("password", password);
        this.post("/auth/login", body.toString(), onSuccess, onError);
    }

    public void logout(String username,
                       Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        ObjectNode body = ClientThread.MAPPER.createObjectNode();
        body.put("username", username);
        this.post("/auth/logout", body.toString(), onSuccess, onError);
    }

    // ── Users ─────────────────────────────────────────────────────────────────

    public void getOnlineUsers(Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        this.get("/users/online", onSuccess, onError);
    }

    public void getUserStats(String username,
                             Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        this.get("/users/" + username + "/stats", onSuccess, onError);
    }

    // ── Friends ───────────────────────────────────────────────────────────────

    public void sendFriendRequest(String username, String targetUsername,
                                  Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        ObjectNode body = ClientThread.MAPPER.createObjectNode();
        body.put("username",       username);
        body.put("targetUsername", targetUsername);
        this.post("/friends/request", body.toString(), onSuccess, onError);
    }

    public void acceptFriendRequest(String username, String requesterUsername,
                                    Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        ObjectNode body = ClientThread.MAPPER.createObjectNode();
        body.put("username",          username);
        body.put("requesterUsername", requesterUsername);
        this.post("/friends/accept", body.toString(), onSuccess, onError);
    }

    public void declineFriendRequest(String username, String requesterUsername,
                                     Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        ObjectNode body = ClientThread.MAPPER.createObjectNode();
        body.put("username",          username);
        body.put("requesterUsername", requesterUsername);
        this.post("/friends/decline", body.toString(), onSuccess, onError);
    }

    public void getFriends(String username,
                           Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        this.get("/friends/" + username, onSuccess, onError);
    }

    // ── Challenges ────────────────────────────────────────────────────────────

    public void sendChallenge(String challenger, String target,
                              Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        ObjectNode body = ClientThread.MAPPER.createObjectNode();
        body.put("challenger", challenger);
        body.put("target",     target);
        this.post("/challenge/send", body.toString(), onSuccess, onError);
    }

    public void acceptChallenge(String accepter, String challenger,
                                Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        ObjectNode body = ClientThread.MAPPER.createObjectNode();
        body.put("accepter",   accepter);
        body.put("challenger", challenger);
        this.post("/challenge/accept", body.toString(), onSuccess, onError);
    }

    public void declineChallenge(String decliner, String challenger,
                                 Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        ObjectNode body = ClientThread.MAPPER.createObjectNode();
        body.put("decliner",   decliner);
        body.put("challenger", challenger);
        this.post("/challenge/decline", body.toString(), onSuccess, onError);
    }

    public void getChallenge(String username,
                             Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        this.get("/challenge/" + username, onSuccess, onError);
    }

    // ── Games ─────────────────────────────────────────────────────────────────

    public void createGame(String player1, String player2, boolean vsAI,
                           Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        ObjectNode body = ClientThread.MAPPER.createObjectNode();
        body.put("player1", player1);
        body.put("vsAI",    vsAI);
        if (player2 != null && !player2.isBlank()) body.put("player2", player2);
        this.post("/game/create", body.toString(), onSuccess, onError);
    }

    public void getActiveGame(String username,
                              Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        this.get("/game/active/" + username, onSuccess, onError);
    }

    public void getGameState(String gameId,
                             Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        this.get("/game/" + gameId, onSuccess, onError);
    }

    public void makeMove(String gameId,
                         int fromRow, int fromCol, int toRow, int toCol,
                         String player,
                         Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        ObjectNode body = ClientThread.MAPPER.createObjectNode();
        body.put("player", player);
        ObjectNode from = ClientThread.MAPPER.createObjectNode();
        from.put("row", fromRow); from.put("col", fromCol);
        ObjectNode to = ClientThread.MAPPER.createObjectNode();
        to.put("row", toRow);     to.put("col", toCol);
        body.set("from", from);
        body.set("to",   to);
        this.post("/game/" + gameId + "/move", body.toString(), onSuccess, onError);
    }

    public void sendChatMessage(String gameId, String player, String message,
                                Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        ObjectNode body = ClientThread.MAPPER.createObjectNode();
        body.put("player",  player);
        body.put("message", message);
        this.post("/game/" + gameId + "/chat", body.toString(), onSuccess, onError);
    }

    public void endGame(String gameId, String winner,
                        Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        ObjectNode body = ClientThread.MAPPER.createObjectNode();
        body.put("winner", winner);
        this.post("/game/" + gameId + "/end", body.toString(), onSuccess, onError);
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
            this.socket       = new Socket(InetAddress.getLocalHost(), ClientThread.PORT);
            this.outputStream = new ObjectOutputStream(this.socket.getOutputStream());
            this.outputStream.flush();
            this.inputStream  = new ObjectInputStream(this.socket.getInputStream());
            this.socket.setTcpNoDelay(true);
        } catch (Exception e) {
            ClientThread.LOG.log(Level.WARNING, "Could not connect to server: " + e.getMessage(), e);
            this.failPending("Could not connect to server: " + e.getMessage());
            return;
        }

        while (!Thread.currentThread().isInterrupted()) {
            try {
                HttpResponse res = (HttpResponse) this.inputStream.readObject();
                this.dispatch(res);
            } catch (Exception e) {
                ClientThread.LOG.log(Level.WARNING, "Connection to server lost: " + e.getMessage());
                this.failPending("Connection to server lost.");
                break;
            }
        }

        this.close();
    }

    // ── Transport ─────────────────────────────────────────────────────────────

    private void get(String path, Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        this.send(new HttpRequest("GET", path), onSuccess, onError);
    }

    private void post(String path, String jsonBody,
                      Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        this.send(new HttpRequest("POST", path, jsonBody), onSuccess, onError);
    }

    private synchronized void send(HttpRequest req,
                                   Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        if (this.outputStream == null) {
            Platform.runLater(() -> onError.accept("Not connected to server."));
            return;
        }

        this.pending = new Callbacks(onSuccess, onError);

        try {
            this.outputStream.writeObject(req);
            this.outputStream.flush();
        } catch (IOException e) {
            this.pending = null;
            Platform.runLater(() -> onError.accept("Failed to send request: " + e.getMessage()));
            ClientThread.LOG.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private void dispatch(HttpResponse res) {
        Callbacks cb;
        synchronized (this) {
            cb            = this.pending;
            this.pending  = null;
        }

        if (cb == null) {
            ClientThread.LOG.warning("Received response with no pending callback");
            return;
        }

        String body = res.getBody();
        try {
            JsonNode json = (body == null || body.isBlank())
                    ? ClientThread.MAPPER.createObjectNode()
                    : ClientThread.MAPPER.readTree(body);

            if (res.isOk()) {
                Platform.runLater(() -> cb.onSuccess().accept(json));
            } else {
                String error = json.has("error")
                        ? json.get("error").asText()
                        : "Server error " + res.getStatusCode();
                Platform.runLater(() -> cb.onError().accept(error));
            }
        } catch (Exception e) {
            Platform.runLater(() -> cb.onError().accept("Failed to parse response: " + e.getMessage()));
        }
    }

    private synchronized void failPending(String reason) {
        if (this.pending != null) {
            Callbacks cb  = this.pending;
            this.pending  = null;
            Platform.runLater(() -> cb.onError().accept(reason));
        }
    }
}

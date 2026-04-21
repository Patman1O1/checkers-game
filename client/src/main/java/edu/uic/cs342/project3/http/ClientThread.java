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
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientThread extends Thread {
    // ── Callback ─────────────────────────────────────────────────────────────────────────────────────────────────────

    private static class Callback {
        // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────
        private final Consumer<JsonNode> onSuccess;

        private final Consumer<String> onError;

        // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────
        private Callback(Consumer<JsonNode> onSuccess, Consumer<String> onError) {
            this.onSuccess = onSuccess;
            this.onError = onError;
        }
    }

    // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────────
    private static final Logger LOGGER = Logger.getLogger(ClientThread.class.getName());

    private static final String HOST = "localhost";

    private static final int DEFAULT_PORT = 8080;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final ClientThread INSTANCE = new ClientThread();

    private Socket socket;

    private ObjectOutputStream outputStream;

    private ObjectInputStream inputStream;

    private Callback pending;

    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────
    private ClientThread() {
        this.setDaemon(true);
        this.setName("http-client");
    }

    // ── Getters ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public static ClientThread getInstance() { return ClientThread.INSTANCE; }

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public void signup(String username, String password, Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        ObjectNode body = ClientThread.OBJECT_MAPPER.createObjectNode();
        body.put("username", username);
        body.put("password", password);
        this.post("/auth/signup", body.toString(), onSuccess, onError);
    }

    public void login(String username, String password, Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        ObjectNode body = ClientThread.OBJECT_MAPPER.createObjectNode();
        body.put("username", username);
        body.put("password", password);
        this.post("/auth/login", body.toString(), onSuccess, onError);
    }

    public void logout(String username, Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        ObjectNode body = ClientThread.OBJECT_MAPPER.createObjectNode();
        body.put("username", username);
        this.post("/auth/logout", body.toString(), onSuccess, onError);
    }

    public void getOnlineUsers(Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        this.get("/users/online", onSuccess, onError);
    }

    public void getUserStats(String username,
                             Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        this.get("/users/" + username + "/stats", onSuccess, onError);
    }

    public void addFriend(String username, String targetUsername,
                          Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        ObjectNode body = ClientThread.OBJECT_MAPPER.createObjectNode();
        body.put("username", username);
        body.put("targetUsername", targetUsername);
        this.post("/friends/add", body.toString(), onSuccess, onError);
    }

    public void removeFriend(String username, String targetUsername,
                             Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        ObjectNode body = ClientThread.OBJECT_MAPPER.createObjectNode();
        body.put("username", username);
        body.put("targetUsername", targetUsername);
        this.post("/friends/remove", body.toString(), onSuccess, onError);
    }

    public void getFriends(String username, Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        this.get(String.format("/friends/%s", username), onSuccess, onError);
    }

    public void sendChallenge(String challenger, String target,
                              Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        ObjectNode body = ClientThread.OBJECT_MAPPER.createObjectNode();
        body.put("challenger", challenger);
        body.put("target",     target);
        this.post("/challenge/send", body.toString(), onSuccess, onError);
    }

    public void acceptChallenge(String accepter, String challenger,
                                Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        ObjectNode body = ClientThread.OBJECT_MAPPER.createObjectNode();
        body.put("accepter",   accepter);
        body.put("challenger", challenger);
        this.post("/challenge/accept", body.toString(), onSuccess, onError);
    }

    public void declineChallenge(String decliner, String challenger,
                                 Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        ObjectNode body = ClientThread.OBJECT_MAPPER.createObjectNode();
        body.put("decliner",   decliner);
        body.put("challenger", challenger);
        this.post("/challenge/decline", body.toString(), onSuccess, onError);
    }

    public void getChallenge(String username, Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        this.get("/challenge/" + username, onSuccess, onError);
    }

    public void createGame(String player1, String player2, boolean vsAI,
                           Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        ObjectNode body = ClientThread.OBJECT_MAPPER.createObjectNode();

        body.put("player1", player1);
        body.put("vsAI", vsAI);

        if (player2 != null && !player2.isBlank()) {
            body.put("player2", player2);
        }
        this.post("/game/create", body.toString(), onSuccess, onError);
    }

    public void getActiveGame(String username, Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        this.get(String.format("/game/active/%s", username), onSuccess, onError);
    }

    public void getGameState(String gameId, Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        this.get(String.format("/game/%s", gameId), onSuccess, onError);
    }

    public void makeMove(String gameId,
                         int fromRow, int fromCol, int toRow, int toCol,
                         String player,
                         Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        ObjectNode body = ClientThread.OBJECT_MAPPER.createObjectNode();
        body.put("player", player);

        ObjectNode from = ClientThread.OBJECT_MAPPER.createObjectNode();
        from.put("row", fromRow); from.put("col", fromCol);

        ObjectNode to = ClientThread.OBJECT_MAPPER.createObjectNode();
        to.put("row", toRow); to.put("col", toCol);

        body.set("from", from);
        body.set("to", to);
        this.post(String.format("/game/%s/move", gameId), body.toString(), onSuccess, onError);
    }

    public void sendChatMessage(String gameId, String player, String message,
                                Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        ObjectNode body = ClientThread.OBJECT_MAPPER.createObjectNode();
        body.put("player",  player);
        body.put("message", message);
        this.post(String.format("/game/%s/chat", gameId), body.toString(), onSuccess, onError);
    }

    public void endGame(String gameId, String winner, Consumer<JsonNode> onSuccess, Consumer<String> onError) {
        ObjectNode body = ClientThread.OBJECT_MAPPER.createObjectNode();
        body.put("winner", winner);
        this.post(String.format("/game/%s/end", gameId), body.toString(), onSuccess, onError);
    }

    public void close() {
        try {
            if (this.inputStream != null) {
                this.inputStream.close();
            }

            if (this.outputStream != null) {
                this.outputStream.close();
            }

            if (this.socket != null) {
                this.socket.close();
            }
        } catch (IOException ignored) {
            // Ignore
        }
    }

    @Override
    public void run() {
        try {
            this.socket = new Socket(InetAddress.getLocalHost(), ClientThread.DEFAULT_PORT);
            this.outputStream = new ObjectOutputStream(this.socket.getOutputStream());
            this.outputStream.flush();
            this.inputStream = new ObjectInputStream(this.socket.getInputStream());
            this.socket.setTcpNoDelay(true);
        } catch (Exception exception) {
            ClientThread.LOGGER.log(Level.WARNING, String.format("Could not connect to server: %s", exception.getMessage()), exception);
            this.failPending(String.format("Could not connect to server: %s", exception.getMessage()));
            return;
        }

        while (!Thread.currentThread().isInterrupted()) {
            try {
                this.dispatch((HttpResponse) this.inputStream.readObject());
            } catch (Exception exception) {
                ClientThread.LOGGER.log(Level.WARNING, "Connection to server lost: " + exception.getMessage());
                this.failPending("Connection to server lost.");
                break;
            }
        }

        this.close();
    }

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

        this.pending = new Callback(onSuccess, onError);

        try {
            this.outputStream.writeObject(req);
            this.outputStream.flush();
        } catch (IOException exception) {
            this.pending = null;
            Platform.runLater(() -> onError.accept(String.format("Failed to send request: %s", exception.getMessage())));
            ClientThread.LOGGER.log(Level.SEVERE, exception.getMessage(), exception);
        }
    }

    private void dispatch(HttpResponse response) {
        Callback callback;
        synchronized (this) {
            callback = this.pending;
            this.pending  = null;
        }

        if (callback == null) {
            ClientThread.LOGGER.warning("Received response with no pending callback");
            return;
        }

        String body = response.getBody();
        try {
            JsonNode json = (body == null || body.isBlank())
                    ? ClientThread.OBJECT_MAPPER.createObjectNode()
                    : ClientThread.OBJECT_MAPPER.readTree(body);

            if (response.isOk()) {
                Platform.runLater(() -> callback.onSuccess.accept(json));
            } else {
                String error = json.has("error")
                        ? json.get("error").asText()
                        : String.format("Server error: %d", response.getStatusCode());
                Platform.runLater(() -> callback.onError.accept(error));
            }
        } catch (Exception exception) {
            Platform.runLater(() -> callback.onError.accept(String.format("Failed to parse response: %s", exception.getMessage())));
        }
    }

    private synchronized void failPending(String reason) {
        if (this.pending != null) {
            Callback callback = this.pending;
            this.pending = null;
            Platform.runLater(() -> callback.onError.accept(reason));
        }
    }
}
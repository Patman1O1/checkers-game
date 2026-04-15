package edu.uic.cs342.project3.controller;

import edu.uic.cs342.project3.SceneManager;
import edu.uic.cs342.project3.http.ClientThread;
import com.fasterxml.jackson.databind.JsonNode;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Controller for lobby.fxml.
 * Calls ClientThread directly for all network operations.
 */
public class LobbyController {

    // ── Fields ────────────────────────────────────────────────────────────────

    @FXML private Label            welcomeLabel;
    @FXML private Label            winsLabel;
    @FXML private Label            lossesLabel;
    @FXML private Label            drawsLabel;
    @FXML private Label            winRateLabel;
    @FXML private Label            statusLabel;
    @FXML private ListView<String> onlineUsersList;
    @FXML private ListView<String> friendsList;
    @FXML private TextField        addFriendField;
    @FXML private Button           challengeButton;
    @FXML private Button           challengeFriendButton;

    private SceneManager sceneManager;
    private ClientThread client;
    private String       selectedOpponent;

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "lobby-poller");
                t.setDaemon(true);
                return t;
            });
    private ScheduledFuture<?> pollFuture;

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setSceneManager(SceneManager sm) { this.sceneManager = sm; }
    public void setClientThread(ClientThread ct) { this.client       = ct; }

    // ── Methods ───────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        challengeButton.setDisable(true);
        challengeFriendButton.setDisable(true);

        onlineUsersList.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, val) -> {
                    selectedOpponent = val;
                    challengeButton.setDisable(val == null);
                });
    }

    public void onEnter() {
        welcomeLabel.setText("Welcome, " + sceneManager.getCurrentUsername() + "!");
        challengeButton.setDisable(true);
        selectedOpponent = null;
        onlineUsersList.getSelectionModel().clearSelection();
        loadData();
        startPolling();
    }

    public void onLeave() {
        stopPolling();
    }

    @FXML
    private void handlePlayVsAI() {
        startGame(null, true);
    }

    @FXML
    private void handleChallengeSelected() {
        if (selectedOpponent != null) startGame(selectedOpponent, false);
    }

    @FXML
    private void handleChallengeFriend() {
        String selected = friendsList.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        String name = selected.replaceFirst("^[\u25cf\u25cb] ", "").split("\\s+")[0];
        startGame(name, false);
    }

    @FXML
    private void handleAddFriend() {
        String friend = addFriendField.getText().trim();
        if (friend.isBlank()) { showStatus("Enter a username.", true); return; }

        String username = sceneManager.getCurrentUsername();
        client.addFriend(username, friend,
            json -> {
                addFriendField.clear();
                showStatus("Friend added!", false);
                loadData();
            },
            err -> showStatus(err, true));
    }

    @FXML
    private void handleLogout() {
        stopPolling();
        client.logout(sceneManager.getCurrentUsername(),
            json -> sceneManager.showLogin(),
            err  -> sceneManager.showLogin());
    }

    private void loadData() {
        String username = sceneManager.getCurrentUsername();

        client.getOnlineUsers(
            json -> {
                onlineUsersList.getItems().clear();
                for (JsonNode u : json.path("users")) {
                    String name = u.asText();
                    if (!name.equals(username))
                        onlineUsersList.getItems().add(name);
                }
            },
            err -> showStatus("Could not load users: " + err, true));

        client.getFriends(username,
            json -> {
                friendsList.getItems().clear();
                for (JsonNode f : json.path("friends")) {
                    String  name   = f.path("username").asText();
                    boolean online = f.path("isOnline").asBoolean();
                    int     w      = f.path("stats").path("wins").asInt();
                    int     l      = f.path("stats").path("losses").asInt();
                    int     d      = f.path("stats").path("draws").asInt();
                    friendsList.getItems().add(
                            (online ? "\u25cf " : "\u25cb ") + name
                            + "  W:" + w + " L:" + l + " D:" + d);
                }
            },
            err -> showStatus("Could not load friends: " + err, true));

        client.getUserStats(username,
            json -> {
                JsonNode s      = json.path("stats");
                int      wins   = s.path("wins").asInt();
                int      losses = s.path("losses").asInt();
                int      draws  = s.path("draws").asInt();
                int      total  = wins + losses + draws;
                double   rate   = total > 0 ? (wins * 100.0 / total) : 0.0;
                winsLabel.setText(String.valueOf(wins));
                lossesLabel.setText(String.valueOf(losses));
                drawsLabel.setText(String.valueOf(draws));
                winRateLabel.setText(String.format("%.1f%%", rate));
            },
            err -> showStatus("Could not load stats: " + err, true));
    }

    private void startGame(String opponent, boolean vsAI) {
        String username = sceneManager.getCurrentUsername();
        client.createGame(username, opponent, vsAI,
            json -> {
                String gameId = json.path("gameId").asText();
                onLeave();
                sceneManager.showGame(gameId);
            },
            err -> showStatus("Failed to create game: " + err, true));
    }

    private void startPolling() {
        stopPolling();
        pollFuture = scheduler.scheduleAtFixedRate(this::loadData, 5, 5, TimeUnit.SECONDS);
    }

    private void stopPolling() {
        if (pollFuture != null) { pollFuture.cancel(false); pollFuture = null; }
    }

    private void showStatus(String msg, boolean error) {
        statusLabel.setText(msg);
        statusLabel.setStyle(error ? "-fx-text-fill: #f87171;" : "-fx-text-fill: #4ade80;");
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }
}

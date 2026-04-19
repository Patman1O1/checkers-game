package edu.uic.cs342.controller;

import edu.uic.cs342.SceneManager;
import edu.uic.cs342.http.ClientThread;
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

public class LobbyController {

    // ── Fields ────────────────────────────────────────────────────────────────

    @FXML private Label            welcomeLabel;
    @FXML private Label            winsLabel;
    @FXML private Label            lossesLabel;
    @FXML private Label            drawsLabel;
    @FXML private Label            winRateLabel;
    @FXML private Label            statusLabel;

    @FXML private ListView<String> onlineUsersList;
    @FXML private Button           challengeButton;

    @FXML private ListView<String> friendsList;
    @FXML private Button           challengeFriendButton;

    @FXML private TextField        friendRequestField;
    @FXML private ListView<String> incomingRequestsList;

    @FXML private Label            challengeLabel;
    @FXML private Button           acceptChallengeButton;
    @FXML private Button           declineChallengeButton;

    private SceneManager sceneManager;
    private ClientThread client;
    private String       selectedOpponent;
    private String       pendingChallenger;

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
        this.challengeButton.setDisable(true);
        this.hideChallengePanel();

        this.onlineUsersList.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, val) -> {
                    this.selectedOpponent = val;
                    this.challengeButton.setDisable(val == null);
                });
    }

    public void onEnter() {
        this.welcomeLabel.setText("Welcome, " + this.sceneManager.getCurrentUsername() + "!");
        this.challengeButton.setDisable(true);
        this.selectedOpponent  = null;
        this.pendingChallenger = null;
        this.onlineUsersList.getSelectionModel().clearSelection();
        this.hideChallengePanel();
        this.loadData();
        this.startPolling();
    }

    public void onLeave() {
        this.stopPolling();
    }

    // ── Online player challenge ───────────────────────────────────────────────

    @FXML
    private void handleChallengeSelected() {
        if (this.selectedOpponent == null) return;
        this.client.sendChallenge(this.sceneManager.getCurrentUsername(), this.selectedOpponent,
            json -> this.showStatus("Challenge sent to " + this.selectedOpponent + "!", false),
            err  -> this.showStatus("Could not send challenge: " + err, true));
    }

    // ── Friend challenge ──────────────────────────────────────────────────────

    @FXML
    private void handleChallengeFriend() {
        String selected = this.friendsList.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        String name = selected.replaceFirst("^[\u25cf\u25cb] ", "").split("\\s+")[0];
        this.client.sendChallenge(this.sceneManager.getCurrentUsername(), name,
            json -> this.showStatus("Challenge sent to " + name + "!", false),
            err  -> this.showStatus("Could not send challenge: " + err, true));
    }

    // ── Incoming challenge ────────────────────────────────────────────────────

    @FXML
    private void handleAcceptChallenge() {
        if (this.pendingChallenger == null) return;
        String challenger = this.pendingChallenger;
        this.client.acceptChallenge(this.sceneManager.getCurrentUsername(), challenger,
            json -> {
                String gameId = json.path("gameId").asText();
                this.onLeave();
                this.sceneManager.showGame(gameId);
            },
            err -> this.showStatus("Could not accept challenge: " + err, true));
    }

    @FXML
    private void handleDeclineChallenge() {
        if (this.pendingChallenger == null) return;
        String challenger = this.pendingChallenger;
        this.client.declineChallenge(this.sceneManager.getCurrentUsername(), challenger,
            json -> {
                this.pendingChallenger = null;
                this.hideChallengePanel();
                this.showStatus(challenger + "'s challenge declined.", false);
            },
            err -> this.showStatus("Error: " + err, true));
    }

    // ── Friend requests ───────────────────────────────────────────────────────

    @FXML
    private void handleSendFriendRequest() {
        String target = this.friendRequestField.getText().trim();
        if (target.isBlank()) { this.showStatus("Enter a username.", true); return; }
        this.client.sendFriendRequest(this.sceneManager.getCurrentUsername(), target,
            json -> {
                this.friendRequestField.clear();
                boolean auto = json.path("autoAccepted").asBoolean(false);
                this.showStatus(auto ? target + " is now your friend!"
                                     : "Friend request sent to " + target + ".", false);
                this.loadData();
            },
            err -> this.showStatus(err, true));
    }

    @FXML
    private void handleAcceptFriendRequest() {
        String selected = this.incomingRequestsList.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        this.client.acceptFriendRequest(this.sceneManager.getCurrentUsername(), selected,
            json -> { this.showStatus(selected + " is now your friend!", false); this.loadData(); },
            err  -> this.showStatus(err, true));
    }

    @FXML
    private void handleDeclineFriendRequest() {
        String selected = this.incomingRequestsList.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        this.client.declineFriendRequest(this.sceneManager.getCurrentUsername(), selected,
            json -> { this.showStatus("Request from " + selected + " declined.", false); this.loadData(); },
            err  -> this.showStatus(err, true));
    }

    // ── AI game ───────────────────────────────────────────────────────────────

    @FXML
    private void handlePlayVsAI() {
        this.client.createGame(this.sceneManager.getCurrentUsername(), null, true,
            json -> { this.onLeave(); this.sceneManager.showGame(json.path("gameId").asText()); },
            err  -> this.showStatus("Failed to create game: " + err, true));
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @FXML
    private void handleLogout() {
        this.stopPolling();
        this.client.logout(this.sceneManager.getCurrentUsername(),
            json -> this.sceneManager.showLogin(),
            err  -> this.sceneManager.showLogin());
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void loadData() {
        String username = this.sceneManager.getCurrentUsername();
        this.loadOnlineUsers(username);
    }

    private void loadOnlineUsers(String username) {
        this.client.getOnlineUsers(
            json -> {
                this.onlineUsersList.getItems().clear();
                for (JsonNode u : json.path("users")) {
                    String name = u.asText();
                    if (!name.equals(username))
                        this.onlineUsersList.getItems().add(name);
                }
                this.loadFriends(username);
            },
            err -> {
                this.showStatus("Could not load users: " + err, true);
                this.loadFriends(username);
            });
    }

    private void loadFriends(String username) {
        this.client.getFriends(username,
            json -> {
                this.friendsList.getItems().clear();
                for (JsonNode f : json.path("friends")) {
                    String  name   = f.path("username").asText();
                    boolean online = f.path("isOnline").asBoolean();
                    int     w = f.path("stats").path("wins").asInt();
                    int     l = f.path("stats").path("losses").asInt();
                    int     d = f.path("stats").path("draws").asInt();
                    this.friendsList.getItems().add(
                            (online ? "\u25cf " : "\u25cb ") + name
                            + "  W:" + w + " L:" + l + " D:" + d);
                }
                this.incomingRequestsList.getItems().clear();
                for (JsonNode r : json.path("incoming"))
                    this.incomingRequestsList.getItems().add(r.asText());
                this.loadStats(username);
            },
            err -> {
                this.showStatus("Could not load friends: " + err, true);
                this.loadStats(username);
            });
    }

    private void loadStats(String username) {
        this.client.getUserStats(username,
            json -> {
                JsonNode s  = json.path("stats");
                int wins    = s.path("wins").asInt();
                int losses  = s.path("losses").asInt();
                int draws   = s.path("draws").asInt();
                int total   = wins + losses + draws;
                this.winsLabel.setText(String.valueOf(wins));
                this.lossesLabel.setText(String.valueOf(losses));
                this.drawsLabel.setText(String.valueOf(draws));
                this.winRateLabel.setText(total > 0
                        ? String.format("%.1f%%", wins * 100.0 / total) : "0.0%");
                this.loadChallenge(username);
            },
            err -> {
                this.showStatus("Could not load stats: " + err, true);
                this.loadChallenge(username);
            });
    }

    private void loadChallenge(String username) {
        this.client.getChallenge(username,
            json -> {
                String challenger = json.path("challenger").asText(null);
                if (challenger != null && !challenger.isBlank()) {
                    this.pendingChallenger = challenger;
                    this.showChallengePanel(challenger);
                } else if (this.pendingChallenger != null) {
                    this.pendingChallenger = null;
                    this.hideChallengePanel();
                }
                this.loadActiveGame(username);
            },
            err -> this.loadActiveGame(username));
    }

    private void loadActiveGame(String username) {
        this.client.getActiveGame(username,
            json -> {
                String gameId = json.path("gameId").asText(null);
                if (gameId != null && !gameId.isBlank()) {
                    this.onLeave();
                    this.sceneManager.showGame(gameId);
                }
            },
            err -> { /* silent */ });
    }

    // ── Polling ───────────────────────────────────────────────────────────────

    private void startPolling() {
        this.stopPolling();
        this.pollFuture = this.scheduler.scheduleAtFixedRate(this::loadData, 3, 3, TimeUnit.SECONDS);
    }

    private void stopPolling() {
        if (this.pollFuture != null) { this.pollFuture.cancel(false); this.pollFuture = null; }
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private void showChallengePanel(String challenger) {
        this.challengeLabel.setText(challenger + " challenged you!");
        this.challengeLabel.setVisible(true);        this.challengeLabel.setManaged(true);
        this.acceptChallengeButton.setVisible(true);  this.acceptChallengeButton.setManaged(true);
        this.declineChallengeButton.setVisible(true); this.declineChallengeButton.setManaged(true);
    }

    private void hideChallengePanel() {
        this.challengeLabel.setVisible(false);        this.challengeLabel.setManaged(false);
        this.acceptChallengeButton.setVisible(false);  this.acceptChallengeButton.setManaged(false);
        this.declineChallengeButton.setVisible(false); this.declineChallengeButton.setManaged(false);
    }

    private void showStatus(String msg, boolean error) {
        this.statusLabel.setText(msg);
        this.statusLabel.setStyle(error ? "-fx-text-fill: #f87171;" : "-fx-text-fill: #4ade80;");
        this.statusLabel.setVisible(true);
        this.statusLabel.setManaged(true);
    }
}

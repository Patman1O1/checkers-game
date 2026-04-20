package edu.uic.cs342.project3.controllers;

import edu.uic.cs342.project3.SceneManager;
import edu.uic.cs342.project3.http.ClientThread;

import com.fasterxml.jackson.databind.JsonNode;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class LobbyController {
    // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────────
    private final ScheduledExecutorService scheduler;

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label winsLabel;

    @FXML
    private Label lossesLabel;

    @FXML
    private Label drawsLabel;

    @FXML
    private Label winRateLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private ListView<String> onlineUsersList;

    @FXML
    private Button challengeButton;

    @FXML
    private ListView<String> friendsList;

    @FXML
    private Button challengeFriendButton;

    @FXML
    private TextField friendRequestField;

    @FXML
    private ListView<String> incomingRequestsList;

    @FXML
    private Label  challengeLabel;

    @FXML
    private Button acceptChallengeButton;

    @FXML
    private Button declineChallengeButton;

    private SceneManager sceneManager;

    private ClientThread clientThread;

    private String selectedOpponent, pendingChallenger;

    private ScheduledFuture<?> pollFuture;

    public static final URL SCENE_FXML = LobbyController.class.getResource("/fxml/lobby.fxml");

    public static final double SCENE_WIDTH = 1100.0;

    public static final double SCENE_HEIGHT = 700.0;

    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────
    public LobbyController() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "lobby-poller");
            thread.setDaemon(true);
            return thread;
        });
    }

    // ── Setters ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public void setSceneManager(SceneManager sceneManager) { this.sceneManager = sceneManager; }

    public void setClientThread(ClientThread clientThread) { this.clientThread = clientThread; }

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    @FXML
    private void handleChallengeSelected() {
        if (this.selectedOpponent == null) {
            return;
        }
        this.clientThread.sendChallenge(this.sceneManager.getCurrentUsername(), this.selectedOpponent,
                json -> this.showStatus(String.format("Challenge sent to %s!", this.selectedOpponent), false),
                error -> this.showStatus(String.format("Could not send challenge: %s", error), true));
    }

    @FXML
    private void handleChallengeFriend() {
        String selectedFriendName = this.friendsList.getSelectionModel().getSelectedItem();
        if (selectedFriendName == null) {
            return;
        }

        String friendName = selectedFriendName.replaceFirst("^[\u25cf\u25cb] ", "").split("\\s+")[0];
        this.clientThread.sendChallenge(this.sceneManager.getCurrentUsername(), friendName,
                json -> this.showStatus(String.format("Challenge sent to %s !", friendName), false),
                error -> this.showStatus(String.format("Could not send challenge: %s", error), true));
    }

    @FXML
    private void handleAcceptChallenge() {
        if (this.pendingChallenger == null) {
            return;
        }
        String challenger = this.pendingChallenger;
        this.clientThread.acceptChallenge(this.sceneManager.getCurrentUsername(), challenger,
                json -> {
                    String gameId = json.path("gameId").asText();
                    this.onLeave();
                    this.sceneManager.showGame(gameId);
                },
                error -> this.showStatus(String.format("Could not accept challenge: %s", error), true));
    }

    @FXML
    private void handleDeclineChallenge() {
        if (this.pendingChallenger == null) {
            return;
        }

        String challenger = this.pendingChallenger;
        this.clientThread.declineChallenge(this.sceneManager.getCurrentUsername(), challenger,
                json -> {
                    this.pendingChallenger = null;
                    this.hideChallengePanel();
                    this.showStatus(String.format("%s's challenge declined.", challenger), false);
                },
                error -> this.showStatus(String.format("Error: %s", error), true));
    }

    @FXML
    private void handleSendFriendRequest() {
        String target = this.friendRequestField.getText().trim();
        if (target.isBlank()) {
            this.showStatus("Enter a username.", true);
            return;
        }
        this.clientThread.sendFriendRequest(this.sceneManager.getCurrentUsername(), target,
                json -> {
                    this.friendRequestField.clear();
                    if (json.path("autoAccepted").asBoolean(false)) {
                        this.showStatus(String.format("%s is now your friend!", target), false);
                    } else {
                        this.showStatus(String.format("Friend request sent to %s.", target), false);
                    }
                    this.loadData();
                },
                error -> this.showStatus(error, true));
    }

    @FXML
    private void handleAcceptFriendRequest() {
        String selected = this.incomingRequestsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        this.clientThread.acceptFriendRequest(this.sceneManager.getCurrentUsername(), selected,
                json -> {
                    this.showStatus(String.format("%s is now your friend!", selected), false);
                    this.loadData();
            },
                error -> this.showStatus(error, true));
    }

    @FXML
    private void handleDeclineFriendRequest() {
        String selected = this.incomingRequestsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        this.clientThread.declineFriendRequest(this.sceneManager.getCurrentUsername(), selected,
                json -> {
                    this.showStatus(String.format("Request from %s declined.", selected), false);
                    this.loadData();
                },
                error -> this.showStatus(error, true));
    }

    @FXML
    private void handlePlayVsAI() {
        this.clientThread.createGame(this.sceneManager.getCurrentUsername(), null, true,
                json -> {
                    this.onLeave();
                    this.sceneManager.showGame(json.path("gameId").asText());
                },
                error -> this.showStatus(String.format("Failed to create game: %s", error), true));
    }

    @FXML
    private void handleLogout() {
        this.stopPolling();
        this.clientThread.logout(this.sceneManager.getCurrentUsername(),
                json -> this.sceneManager.showLogin(),
                error  -> this.sceneManager.showLogin());
    }

    private void loadData() {
        String username = this.sceneManager.getCurrentUsername();
        this.clientThread.getOnlineUsers(
                jsonNode -> {
                    this.onlineUsersList.getItems().clear();
                    for (JsonNode userNode : jsonNode.path("users")) {
                        String name = userNode.asText();
                        if (!name.equals(username)) {
                            this.onlineUsersList.getItems().add(name);
                        }
                    }
                    this.loadFriends(username);
                },
                error -> {
                    this.showStatus(String.format("Could not load users: %s", error), true);
                    this.loadFriends(username);
                });
    }

    private void loadFriends(String username) {
        this.clientThread.getFriends(username,
                jsonNode -> {
                    this.friendsList.getItems().clear();
                    for (JsonNode friendNode : jsonNode.path("friends")) {
                        String name = friendNode.path("username").asText();

                        int wins = friendNode.path("stats").path("wins").asInt();
                        int losses = friendNode.path("stats").path("losses").asInt();
                        int draws = friendNode.path("stats").path("draws").asInt();

                        if (friendNode.path("isOnline").asBoolean()) {
                            this.friendsList.getItems().add(String.format("\u25cf %s W:%d L:%d D:%d", name, wins, losses, draws));
                        } else {
                            this.friendsList.getItems().add(String.format("\u25cb %s W:%d L:%d D:%d", name, wins, losses, draws));
                        }
                    }
                    this.incomingRequestsList.getItems().clear();
                    for (JsonNode requestNode : jsonNode.path("incoming")) {
                        this.incomingRequestsList.getItems().add(requestNode.asText());
                    }
                    this.loadStats(username);
                },
                error -> {
                    this.showStatus(String.format("Could not load friends: %s", error), false);
                    this.loadStats(username);
                });
    }

    private void loadStats(String username) {
        this.clientThread.getUserStats(username,
                json -> {
                    JsonNode statsNode = json.path("stats");
                    int wins = statsNode.path("wins").asInt();
                    int losses = statsNode.path("losses").asInt();
                    int draws = statsNode.path("draws").asInt();
                    int total = wins + losses + draws;

                    this.winsLabel.setText(String.valueOf(wins));
                    this.lossesLabel.setText(String.valueOf(losses));
                    this.drawsLabel.setText(String.valueOf(draws));
                    if (total > 0) {
                        this.winRateLabel.setText(String.format("%.1f%%", wins * 100.0 / total));
                    } else {
                        this.winRateLabel.setText("0.0%");
                    }
                    this.loadChallenge(username);
                },
                error -> {
                    this.showStatus(String.format("Could not load stats: %s", error), true);
                    this.loadChallenge(username);
                });
    }

    private void loadChallenge(String username) {
        this.clientThread.getChallenge(username,
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
                error -> this.loadActiveGame(username));
    }

    private void loadActiveGame(String username) {
        this.clientThread.getActiveGame(username,
                json -> {
                    String gameId = json.path("gameId").asText(null);
                    if (gameId != null && !gameId.isBlank()) {
                        this.onLeave();
                        this.sceneManager.showGame(gameId);
                    }
                },
                error -> { /* silent */ });
    }

    private void startPolling() {
        this.stopPolling();
        this.pollFuture = this.scheduler.scheduleAtFixedRate(this::loadData, 3L, 3L, TimeUnit.SECONDS);
    }

    private void stopPolling() {
        if (this.pollFuture != null) {
            this.pollFuture.cancel(false);
            this.pollFuture = null;
        }
    }

    private void showChallengePanel(String challenger) {
        this.challengeLabel.setText(String.format("%s challenged you!", challenger));

        this.challengeLabel.setVisible(true);
        this.challengeLabel.setManaged(true);

        this.acceptChallengeButton.setVisible(true);
        this.acceptChallengeButton.setManaged(true);

        this.declineChallengeButton.setVisible(true);
        this.declineChallengeButton.setManaged(true);
    }

    private void hideChallengePanel() {
        this.challengeLabel.setVisible(false);
        this.challengeLabel.setManaged(false);

        this.acceptChallengeButton.setVisible(false);
        this.acceptChallengeButton.setManaged(false);

        this.declineChallengeButton.setVisible(false);
        this.declineChallengeButton.setManaged(false);
    }

    private void showStatus(String message, boolean wasError) {
        this.statusLabel.setText(message);

        if (wasError) {
            this.statusLabel.setStyle("-fx-text-fill: #f87171;");
        } else {
            this.statusLabel.setStyle("-fx-text-fill: #4ade80;");
        }

        this.statusLabel.setVisible(true);
        this.statusLabel.setManaged(true);
    }

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
        this.welcomeLabel.setText(String.format("Welcome, %s!", this.sceneManager.getCurrentUsername()));
        this.challengeButton.setDisable(true);

        this.selectedOpponent = null;
        this.pendingChallenger = null;

        this.onlineUsersList.getSelectionModel().clearSelection();
        this.hideChallengePanel();
        this.loadData();
        this.startPolling();
    }

    public void onLeave() { this.stopPolling(); }
}

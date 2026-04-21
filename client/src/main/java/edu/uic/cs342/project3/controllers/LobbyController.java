package edu.uic.cs342.project3.controllers;

import edu.uic.cs342.project3.SceneManager;
import edu.uic.cs342.project3.http.ClientThread;

import com.fasterxml.jackson.databind.JsonNode;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

import java.net.URL;
import java.util.LinkedHashSet;
import java.util.SequencedCollection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class LobbyController {
    // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────────
    private final ScheduledExecutorService scheduler;

    @FXML private Label            welcomeLabel;
    @FXML private Label            winsLabel;
    @FXML private Label            lossesLabel;
    @FXML private Label            drawsLabel;
    @FXML private Label            winRateLabel;
    @FXML private Label            statusLabel;
    @FXML private ListView<String> onlineUsersList;
    @FXML private ListView<String> friendsList;
    @FXML private ListView<String> challengesList;
    @FXML private TextField        friendRequestField;

    private SceneManager sceneManager;
    private ClientThread clientThread;
    private ScheduledFuture<?> pollFuture;
    private Popup activePopup;

    /**
     * Accumulated set of usernames who have sent an unresolved challenge.
     * A LinkedHashSet keeps insertion order and prevents duplicates.
     */
    private final LinkedHashSet<String> pendingChallengers = new LinkedHashSet<>();

    /** Game ID that the user voluntarily left — skip re-navigation to it. */
    private String exitedGameId;

    public static final URL    SCENE_FXML   = LobbyController.class.getResource("/fxml/lobby.fxml");
    public static final double SCENE_WIDTH  = 1100.0;
    public static final double SCENE_HEIGHT = 700.0;

    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────
    public LobbyController() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "lobby-poller");
            t.setDaemon(true);
            return t;
        });
    }

    // ── Setters ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public void setSceneManager(SceneManager sm) { this.sceneManager = sm; }
    public void setClientThread(ClientThread ct)  { this.clientThread = ct; }

    // ── FXML handlers ────────────────────────────────────────────────────────────────────────────────────────────────

    @FXML
    private void handleAddFriendByField() {
        String target = this.friendRequestField.getText().trim();
        if (target.isBlank()) { this.showStatus("Enter a username.", true); return; }
        this.clientThread.addFriend(this.sceneManager.getCurrentUsername(), target,
                json -> {
                    this.friendRequestField.clear();
                    this.showStatus(String.format("%s added as a friend!", target), false);
                    this.loadData();
                },
                error -> this.showStatus(error, true));
    }

    @FXML
    private void handlePlayVsAI() {
        this.clientThread.createGame(this.sceneManager.getCurrentUsername(), null, true,
                json -> { this.onLeave(); this.sceneManager.showGame(json.path("gameId").asText()); },
                error -> this.showStatus(String.format("Failed to create game: %s", error), true));
    }

    @FXML
    private void handleLogout() {
        this.stopPolling();
        this.clientThread.logout(this.sceneManager.getCurrentUsername(),
                json -> this.sceneManager.showLogin(),
                error -> this.sceneManager.showLogin());
    }

    // ── Popup factory ─────────────────────────────────────────────────────────────────────────────────────────────────

    private void showPopup(ListView<String> anchor, String title, Button... buttons) {
        this.closeActivePopup();

        Label header = new Label(title);
        header.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #e5e7eb;");

        VBox box = new VBox(8, header, new Separator());
        box.setPadding(new Insets(12));
        box.setStyle(
                "-fx-background-color: #1f2937;" +
                        "-fx-border-color: #374151;"     +
                        "-fx-border-radius: 6;"          +
                        "-fx-background-radius: 6;"      +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 12, 0, 0, 4);"
        );
        for (Button btn : buttons) {
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setPrefWidth(210);
            box.getChildren().add(btn);
        }

        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.getContent().add(box);
        popup.setOnHidden(e -> { if (this.activePopup == popup) this.activePopup = null; });
        this.activePopup = popup;

        javafx.geometry.Bounds b = anchor.localToScreen(anchor.getBoundsInLocal());
        if (b != null) {
            popup.show(anchor, b.getMinX() + b.getWidth() / 2 - 105, b.getMinY() + b.getHeight() / 2);
        }
    }

    private void closeActivePopup() {
        if (this.activePopup != null) { this.activePopup.hide(); this.activePopup = null; }
    }

    // ── Online-list popup ─────────────────────────────────────────────────────────────────────────────────────────────

    private void handleOnlineUserClick(String name) {
        Button addBtn = styledButton("\u2795  Add Friend", "#1d4ed8");
        addBtn.setOnAction(e -> {
            this.closeActivePopup();
            this.clientThread.addFriend(this.sceneManager.getCurrentUsername(), name,
                    json -> { this.showStatus(String.format("%s added as a friend!", name), false); this.loadData(); },
                    error -> this.showStatus(error, true));
        });

        Button challengeBtn = styledButton("\u2694  Challenge", "#b45309");
        challengeBtn.setOnAction(e -> {
            this.closeActivePopup();
            this.clientThread.sendChallenge(this.sceneManager.getCurrentUsername(), name,
                    json -> this.showStatus(String.format("Challenge sent to %s!", name), false),
                    error -> this.showStatus(String.format("Could not challenge: %s", error), true));
        });

        this.showPopup(this.onlineUsersList, name, addBtn, challengeBtn);
    }

    // ── Friends-list popup ────────────────────────────────────────────────────────────────────────────────────────────

    private void handleFriendClick(String item) {
        String name = item.replaceFirst("^[\u25cf\u25cb]\\s*", "").split("\\s+")[0];

        Button challengeBtn = styledButton("\u2694  Challenge", "#b45309");
        challengeBtn.setOnAction(e -> {
            this.closeActivePopup();
            this.clientThread.sendChallenge(this.sceneManager.getCurrentUsername(), name,
                    json -> this.showStatus(String.format("Challenge sent to %s!", name), false),
                    error -> this.showStatus(String.format("Could not challenge: %s", error), true));
        });

        Button removeBtn = styledButton("\uD83D\uDDD1  Remove Friend", "#7f1d1d");
        removeBtn.setOnAction(e -> {
            this.closeActivePopup();
            this.clientThread.removeFriend(this.sceneManager.getCurrentUsername(), name,
                    json -> { this.showStatus(String.format("%s removed from friends.", name), false); this.loadData(); },
                    error -> this.showStatus(error, true));
        });

        this.showPopup(this.friendsList, name, challengeBtn, removeBtn);
    }

    // ── Challenges-list popup ─────────────────────────────────────────────────────────────────────────────────────────

    private void handleChallengeClick(String challenger) {
        Button acceptBtn = styledButton("\u2705  Accept", "#15803d");
        acceptBtn.setOnAction(e -> {
            this.closeActivePopup();
            this.clientThread.acceptChallenge(this.sceneManager.getCurrentUsername(), challenger,
                    json -> {
                        this.pendingChallengers.remove(challenger);
                        this.refreshChallengesList();
                        this.onLeave();
                        this.sceneManager.showGame(json.path("gameId").asText());
                    },
                    error -> this.showStatus(String.format("Could not accept: %s", error), true));
        });

        Button declineBtn = styledButton("\u274C  Decline", "#7f1d1d");
        declineBtn.setOnAction(e -> {
            this.closeActivePopup();
            this.clientThread.declineChallenge(this.sceneManager.getCurrentUsername(), challenger,
                    json -> {
                        this.pendingChallengers.remove(challenger);
                        this.refreshChallengesList();
                        this.showStatus(String.format("%s's challenge declined.", challenger), false);
                    },
                    error -> this.showStatus(String.format("Could not decline: %s", error), true));
        });

        this.showPopup(this.challengesList,
                String.format("%s challenged you!", challenger),
                acceptBtn, declineBtn);
    }

    // ── Challenges list refresh ───────────────────────────────────────────────────────────────────────────────────────

    private void refreshChallengesList() {
        this.challengesList.getItems().setAll(this.pendingChallengers);
    }

    // ── Helper ────────────────────────────────────────────────────────────────────────────────────────────────────────

    private static Button styledButton(String text, String bgColor) {
        Button btn = new Button(text);
        btn.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: white; -fx-background-radius: 4;", bgColor));
        return btn;
    }

    // ── Data loading ─────────────────────────────────────────────────────────────────────────────────────────────────

    private void loadData() {
        String username = this.sceneManager.getCurrentUsername();
        this.clientThread.getOnlineUsers(
                json -> {
                    this.onlineUsersList.getItems().clear();
                    for (JsonNode u : json.path("users")) {
                        String name = u.asText();
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
                json -> {
                    this.friendsList.getItems().clear();
                    for (JsonNode f : json.path("friends")) {
                        String  name   = f.path("username").asText();
                        boolean online = f.path("isOnline").asBoolean();
                        int w = f.path("stats").path("wins").asInt();
                        int l = f.path("stats").path("losses").asInt();
                        int d = f.path("stats").path("draws").asInt();
                        this.friendsList.getItems().add(String.format(
                                "%s %s  W:%d L:%d D:%d",
                                online ? "\u25cf" : "\u25cb", name, w, l, d));
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
                    JsonNode s = json.path("stats");
                    int wins   = s.path("wins").asInt();
                    int losses = s.path("losses").asInt();
                    int draws  = s.path("draws").asInt();
                    int total  = wins + losses + draws;
                    this.winsLabel.setText(String.valueOf(wins));
                    this.lossesLabel.setText(String.valueOf(losses));
                    this.drawsLabel.setText(String.valueOf(draws));
                    this.winRateLabel.setText(total > 0
                            ? String.format("%.1f%%", wins * 100.0 / total) : "0.0%");
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
                        // Add to the set; if new, refresh the list
                        if (this.pendingChallengers.add(challenger)) {
                            this.refreshChallengesList();
                        }
                    }
                    this.loadActiveGame(username);
                },
                error -> this.loadActiveGame(username));
    }

    private void loadActiveGame(String username) {
        this.clientThread.getActiveGame(username,
                json -> {
                    String gameId = json.path("gameId").asText(null);
                    if (gameId != null && !gameId.isBlank() && !gameId.equals(this.exitedGameId)) {
                        this.onLeave();
                        this.sceneManager.showGame(gameId);
                    }
                },
                error -> { /* silent */ });
    }

    // ── Polling ───────────────────────────────────────────────────────────────────────────────────────────────────────

    private void startPolling() {
        this.stopPolling();
        this.pollFuture = this.scheduler.scheduleAtFixedRate(this::loadData, 3L, 3L, TimeUnit.SECONDS);
    }

    private void stopPolling() {
        if (this.pollFuture != null) { this.pollFuture.cancel(false); this.pollFuture = null; }
    }

    // ── UI helpers ────────────────────────────────────────────────────────────────────────────────────────────────────

    private void showStatus(String message, boolean wasError) {
        this.statusLabel.setText(message);
        this.statusLabel.setStyle(wasError ? "-fx-text-fill: #f87171;" : "-fx-text-fill: #4ade80;");
        this.statusLabel.setVisible(true);
        this.statusLabel.setManaged(true);
    }

    // ── FXML lifecycle ────────────────────────────────────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        this.onlineUsersList.setOnMouseClicked(event -> {
            String sel = this.onlineUsersList.getSelectionModel().getSelectedItem();
            if (sel != null) {
                this.handleOnlineUserClick(sel);
            }
        });

        this.friendsList.setOnMouseClicked(event -> {
            String sel = this.friendsList.getSelectionModel().getSelectedItem();
            if (sel != null) {
                this.handleFriendClick(sel);
            }
        });

        this.challengesList.setOnMouseClicked(event -> {
            String sel = this.challengesList.getSelectionModel().getSelectedItem();
            if (sel != null) {
                this.handleChallengeClick(sel);
            }
        });
    }

    public void onEnter() {
        this.welcomeLabel.setText(String.format("Welcome, %s!", this.sceneManager.getCurrentUsername()));
        this.pendingChallengers.clear();
        this.challengesList.getItems().clear();
        this.onlineUsersList.getSelectionModel().clearSelection();
        this.friendsList.getSelectionModel().clearSelection();
        this.challengesList.getSelectionModel().clearSelection();
        this.closeActivePopup();
        this.loadData();
        this.startPolling();
    }

    public void onEnterFromGame(String leftGameId) {
        this.exitedGameId = leftGameId;
        this.onEnter();
    }

    public void onLeave() {
        this.stopPolling();
        this.closeActivePopup();
    }
}
package edu.uic.cs342.project3.controllers;

import edu.uic.cs342.project3.SceneManager;
import edu.uic.cs342.project3.http.ClientThread;

import com.fasterxml.jackson.databind.JsonNode;

import javafx.fxml.FXML;
import javafx.geometry.Bounds;
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
    private ListView<String> friendsList;

    @FXML
    private ListView<String> challengesList;

    @FXML
    private TextField friendRequestField;

    private SceneManager sceneManager;

    private ClientThread clientThread;

    private ScheduledFuture<?> pollFuture;

    private Popup activePopup;

    private final LinkedHashSet<String> pendingChallengers = new LinkedHashSet<>();

    private String exitedGameId;

    public static final URL SCENE_FXML = LobbyController.class.getResource("/fxml/lobby.fxml");

    public static final double SCENE_WIDTH  = 1100.0;

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
    private void handleAddFriendByField() {
        String target = this.friendRequestField.getText().trim();
        if (target.isBlank()) {
            this.showStatus("Enter a username.", true);
            return;
        }

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

    private void showPopup(ListView<String> anchor, String title, Button... buttons) {
        this.closeActivePopup();

        Label header = new Label(title);
        header.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #e5e7eb;");

        VBox box = new VBox(8.0, header, new Separator());
        box.setPadding(new Insets(12.0));
        box.setStyle(
                "-fx-background-color: #1f2937;" +
                        "-fx-border-color: #374151;" +
                        "-fx-border-radius: 6;" +
                        "-fx-background-radius: 6;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 12, 0, 0, 4);"
        );
        for (Button button : buttons) {
            button.setMaxWidth(Double.MAX_VALUE);
            button.setPrefWidth(210.0);
            box.getChildren().add(button);
        }

        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.getContent().add(box);
        popup.setOnHidden(event -> {
            if (this.activePopup == popup) {
                this.activePopup = null;
            }
        });
        this.activePopup = popup;

        Bounds bounds = anchor.localToScreen(anchor.getBoundsInLocal());
        if (bounds != null) {
            popup.show(anchor, bounds.getMinX() + bounds.getWidth() / 2 - 105, bounds.getMinY() + bounds.getHeight() / 2);
        }
    }

    private void closeActivePopup() {
        if (this.activePopup != null) {
            this.activePopup.hide();
            this.activePopup = null;
        }
    }

    private void handleOnlineUserClick(String name) {
        Button addButton = LobbyController.styledButton("\u2795  Add Friend", "#1d4ed8");
        addButton.setOnAction(event -> {
            this.closeActivePopup();
            this.clientThread.addFriend(this.sceneManager.getCurrentUsername(), name,
                    json -> {
                        this.showStatus(String.format("%s added as a friend!", name), false);
                        this.loadData();
                    },
                    error -> this.showStatus(error, true));
        });

        Button challengeButton = LobbyController.styledButton("\u2694  Challenge", "#b45309");
        challengeButton.setOnAction(event -> {
            this.closeActivePopup();
            this.clientThread.sendChallenge(this.sceneManager.getCurrentUsername(), name,
                    json -> this.showStatus(String.format("Challenge sent to %s!", name), false),
                    error -> this.showStatus(String.format("Could not challenge: %s", error), true));
        });

        this.showPopup(this.onlineUsersList, name, addButton, challengeButton);
    }

    private void handleFriendClick(String item) {
        String name = item.replaceFirst("^[\u25cf\u25cb]\\s*", "").split("\\s+")[0];

        Button challengeButton = LobbyController.styledButton("\u2694  Challenge", "#b45309");
        challengeButton.setOnAction(event -> {
            this.closeActivePopup();
            this.clientThread.sendChallenge(this.sceneManager.getCurrentUsername(), name,
                    json -> this.showStatus(String.format("Challenge sent to %s!", name), false),
                    error -> this.showStatus(String.format("Could not challenge: %s", error), true));
        });

        Button removeButton = LobbyController.styledButton("\uD83D\uDDD1  Remove Friend", "#7f1d1d");
        removeButton.setOnAction(event -> {
            this.closeActivePopup();
            this.clientThread.removeFriend(this.sceneManager.getCurrentUsername(), name,
                    json -> {
                        this.showStatus(String.format("%s removed from friends.", name), false);
                        this.loadData();
                    },
                    error -> this.showStatus(error, true));
        });

        this.showPopup(this.friendsList, name, challengeButton, removeButton);
    }

    private void handleChallengeClick(String challenger) {
        Button acceptButton = LobbyController.styledButton("\u2705  Accept", "#15803d");
        acceptButton.setOnAction(event -> {
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

        Button declineButton = LobbyController.styledButton("\u274C  Decline", "#7f1d1d");
        declineButton.setOnAction(event -> {
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
                acceptButton, declineButton);
    }


    private void refreshChallengesList() {
        this.challengesList.getItems().setAll(this.pendingChallengers);
    }

    private static Button styledButton(String text, String bgColor) {
        Button button = new Button(text);
        button.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: white; -fx-background-radius: 4;", bgColor));
        return button;
    }

    private void loadData() {
        String username = this.sceneManager.getCurrentUsername();
        this.clientThread.getOnlineUsers(
                json -> {
                    this.onlineUsersList.getItems().clear();
                    for (JsonNode userNode : json.path("users")) {
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
                json -> {
                    this.friendsList.getItems().clear();
                    for (JsonNode friendNode : json.path("friends")) {
                        String  name   = friendNode.path("username").asText();
                        boolean online = friendNode.path("isOnline").asBoolean();

                        int wins = friendNode.path("stats").path("wins").asInt();
                        int losses = friendNode.path("stats").path("losses").asInt();
                        int draws = friendNode.path("stats").path("draws").asInt();

                        this.friendsList.getItems().add(String.format(
                                "%s %s  W:%draws L:%draws D:%draws",
                                online ? "\u25cf" : "\u25cb", name, wins, losses, draws));
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
                    int wins   = statsNode.path("wins").asInt();
                    int losses = statsNode.path("losses").asInt();
                    int draws  = statsNode.path("draws").asInt();
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


    private void startPolling() {
        this.stopPolling();
        this.pollFuture = this.scheduler.scheduleAtFixedRate(this::loadData, 3L, 3L, TimeUnit.SECONDS);
    }

    private void stopPolling() {
        if (this.pollFuture != null) { this.pollFuture.cancel(false); this.pollFuture = null; }
    }

    private void showStatus(String message, boolean wasError) {
        this.statusLabel.setText(message);
        this.statusLabel.setStyle(wasError ? "-fx-text-fill: #f87171;" : "-fx-text-fill: #4ade80;");
        this.statusLabel.setVisible(true);
        this.statusLabel.setManaged(true);
    }

    @FXML
    public void initialize() {
        this.onlineUsersList.setOnMouseClicked(event -> {
            String selected = this.onlineUsersList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                this.handleOnlineUserClick(selected);
            }
        });

        this.friendsList.setOnMouseClicked(event -> {
            String selected = this.friendsList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                this.handleFriendClick(selected);
            }
        });

        this.challengesList.setOnMouseClicked(event -> {
            String selected = this.challengesList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                this.handleChallengeClick(selected);
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
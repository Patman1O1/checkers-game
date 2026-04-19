package edu.uic.cs342.project3;

import edu.uic.cs342.project3.game.GameManager;
import edu.uic.cs342.project3.http.ServerThread;
import edu.uic.cs342.project3.model.CheckersGame;
import edu.uic.cs342.project3.util.PlayerRegistry;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ServerController {
    // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────────
    private final PlayerRegistry playerRegistry;

    private final GameManager gameManager;

    @FXML
    private ListView<String> logList;

    @FXML
    private Label statusLabel;

    @FXML
    private Label portLabel;

    @FXML
    private Label onlineLabel;

    @FXML
    private Label gamesLabel;

    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────
    public ServerController() {
        this.playerRegistry = PlayerRegistry.getInstance();
        this.gameManager = GameManager.getInstance();
    }

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    @FXML
    private void clearLog() { this.logList.getItems().clear(); }

    @FXML
    private void refreshStats() {
        this.onlineLabel.setText(String.format("Online: %d", this.playerRegistry.getOnlineUsernames().size()));

        long numActiveGames = 0;
        for (CheckersGame game : this.gameManager.getAllGames()) {
            if (game.getStatus() == CheckersGame.Status.ACTIVE) {
                ++numActiveGames;
            }
        }
        this.gamesLabel.setText(String.format("Active Games: %d", numActiveGames));
    }

    @FXML
    public void initialize() {
        this.portLabel.setText(String.format("Port: %d", ServerThread.DEFAULT_PORT));
        this.statusLabel.setText("● Running");
        this.statusLabel.setStyle("-fx-text-fill: #4ade80;");
        this.refreshStats();
    }

    public void appendLog(String message) {
        Platform.runLater(() -> {
            ObservableList<String> logListItems = this.logList.getItems();
            logListItems.add(String.format("[%s] %s", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")), message));
            this.logList.scrollTo(logListItems.size() - 1);
            this.refreshStats();
        });
    }
}

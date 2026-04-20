package edu.uic.cs342;

import edu.uic.cs342.model.GameManager;
import edu.uic.cs342.http.ServerThread;
import edu.uic.cs342.model.CheckersGame;
import edu.uic.cs342.util.PlayerRegistry;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ServerController {
    // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────────
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

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

    public static final URL SCENE_FXML = ServerController.class.getResource("/fxml/server.fxml");

    public static final double SCENE_WIDTH = 820.0;

    public static final double SCENE_HEIGHT = 560.0;

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    @FXML
    private void clearLog() { this.logList.getItems().clear(); }

    @FXML
    private void refreshStats() {
        long numGames = GameManager.getInstance().getAllGames().stream()
                .filter(game -> game.getStatus() == CheckersGame.Status.ACTIVE)
                .count();
        this.onlineLabel.setText(String.format("Online: %d", PlayerRegistry.getInstance().getOnlineUsernames().size()));
        this.gamesLabel.setText(String.format("Active Games: %d", numGames));
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
            this.logList.getItems().add(String.format("[%s] %s", LocalTime.now().format(ServerController.TIME_FMT), message));
            this.logList.scrollTo(this.logList.getItems().size() - 1);
            this.refreshStats();
        });
    }
}

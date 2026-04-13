package edu.uic.cs342.project3;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import com.checkers.server.game.GameManager;
import com.checkers.server.http.HttpServer;
import com.checkers.server.util.OnlineRegistry;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ServerController {
    // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────────
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

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────
    public ServerController() {}

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    @FXML
    private void clearLog() { this.logList.getItems().clear(); }

    @FXML
    private void refreshStats() {
        int online = OnlineRegistry.getInstance().getOnlineUsernames().size();
        long games = GameManager.getInstance().getAllGames().stream()
                .filter(g -> g.getStatus() == com.checkers.server.model.GameSession.Status.ACTIVE)
                .count();
        this.onlineLabel.setText("Online: " + online);
        this.gamesLabel.setText("Active Games: " + games);
    }

    @FXML
    public void initialize() {
        this.portLabel.setText("Port: " + HttpServer.PORT);
        this.statusLabel.setText("● Running");
        this.statusLabel.setStyle("-fx-text-fill: #4ade80;");
        this.refreshStats();
    }

    /** Called by ServerApplication to append a log message. */
    public void appendLog(String message) {
        Platform.runLater(() -> {
            String entry = "[" + LocalTime.now().format(TIME_FMT) + "] " + message;
            this.logList.getItems().add(entry);
            this.logList.scrollTo(logList.getItems().size() - 1);
            this.refreshStats();
        });
    }
}

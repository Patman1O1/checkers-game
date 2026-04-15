package edu.uic.cs342.project3;

import edu.uic.cs342.project3.game.GameManager;
import edu.uic.cs342.project3.http.ServerThread;
import edu.uic.cs342.project3.model.CheckersGame;
import edu.uic.cs342.project3.util.PlayerRegistry;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * JavaFX controller for server.fxml – the server log GUI.
 */
public class ServerController {

    // ── Fields ────────────────────────────────────────────────────────────────

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML private ListView<String> logList;
    @FXML private Label            statusLabel;
    @FXML private Label            portLabel;
    @FXML private Label            onlineLabel;
    @FXML private Label            gamesLabel;

    // ── Methods ───────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        portLabel.setText("Port: " + ServerThread.PORT);
        statusLabel.setText("● Running");
        statusLabel.setStyle("-fx-text-fill: #4ade80;");
        refreshStats();
    }

    public void appendLog(String message) {
        Platform.runLater(() -> {
            String entry = "[" + LocalTime.now().format(TIME_FMT) + "] " + message;
            logList.getItems().add(entry);
            logList.scrollTo(logList.getItems().size() - 1);
            refreshStats();
        });
    }

    @FXML
    private void clearLog() {
        logList.getItems().clear();
    }

    @FXML
    private void refreshStats() {
        int  online = PlayerRegistry.getInstance().getOnlineUsernames().size();
        long games  = GameManager.getInstance().getAllGames().stream()
                .filter(g -> g.getStatus() == CheckersGame.Status.ACTIVE)
                .count();
        onlineLabel.setText("Online: " + online);
        gamesLabel.setText("Active Games: " + games);
    }
}

package edu.uic.cs342;

import edu.uic.cs342.model.GameManager;
import edu.uic.cs342.http.ServerThread;
import edu.uic.cs342.model.CheckersGame;
import edu.uic.cs342.util.PlayerRegistry;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

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
        this.portLabel.setText("Port: " + ServerThread.PORT);
        this.statusLabel.setText("● Running");
        this.statusLabel.setStyle("-fx-text-fill: #4ade80;");
        this.refreshStats();
    }

    public void appendLog(String message) {
        Platform.runLater(() -> {
            String entry = "[" + LocalTime.now().format(ServerController.TIME_FMT) + "] " + message;
            this.logList.getItems().add(entry);
            this.logList.scrollTo(this.logList.getItems().size() - 1);
            this.refreshStats();
        });
    }

    @FXML
    private void clearLog() {
        this.logList.getItems().clear();
    }

    @FXML
    private void refreshStats() {
        int  online = PlayerRegistry.getInstance().getOnlineUsernames().size();
        long games  = GameManager.getInstance().getAllGames().stream()
                .filter(g -> g.getStatus() == CheckersGame.Status.ACTIVE)
                .count();
        this.onlineLabel.setText("Online: " + online);
        this.gamesLabel.setText("Active Games: " + games);
    }
}

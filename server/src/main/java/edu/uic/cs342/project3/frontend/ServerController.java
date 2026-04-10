package edu.uic.cs342.project3.frontend;

import edu.uic.cs342.project3.backend.ServerThread;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class ServerController implements Initializable {

    @FXML private Label    statusLabel;
    @FXML private Region   statusDot;
    @FXML private Button   statusButton;
    @FXML private Label    clientCountLabel;
    @FXML private Label    activeGamesLabel;
    @FXML private Label    totalGamesLabel;
    @FXML private Label    onlineUsersLabel;
    @FXML private Label    uptimeLabel;
    @FXML private Label    clockLabel;
    @FXML private ListView<LogEntry> logList;

    private static ServerController instance;

    private final ObservableList<LogEntry> logItems = FXCollections.observableArrayList();

    // ── New: track the server thread ──────────────────────────────────────────
    private ServerThread serverThread = null;
    private boolean serverRunning = false;

    private long startTimeMillis;

    private static final DateTimeFormatter STAMP_FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final DateTimeFormatter TIME_FMT  = DateTimeFormatter.ofPattern("HH:mm:ss");

    public enum LogType { INFO, SUCCESS, WARN, ERROR, GAME, CHAT }

    public static class LogEntry {
        private final String  timestamp;
        private final LogType type;
        private final String  message;

        public LogEntry(String timestamp, LogType type, String message) {
            this.timestamp = timestamp;
            this.type      = type;
            this.message   = message;
        }

        public String  getTimestamp() { return timestamp; }
        public LogType getType()      { return type; }
        public String  getMessage()   { return message; }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        instance        = this;
        startTimeMillis = System.currentTimeMillis();

        logList.setItems(logItems);
        logList.setCellFactory(lv -> new LogCellFactory());

        logItems.addListener((ListChangeListener<LogEntry>) change -> {
            if (!logItems.isEmpty()) logList.scrollTo(logItems.size() - 1);
        });

        Timeline ticker = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
        ticker.setCycleCount(Animation.INDEFINITE);
        ticker.play();

        log("╔══════════════════════════════════════════════╗", LogType.SUCCESS);
        log("║        CHECKERS SERVER  v1.0.0               ║", LogType.SUCCESS);
        log("║        UIC CS342 — Project 3                 ║", LogType.SUCCESS);
        log("╚══════════════════════════════════════════════╝", LogType.SUCCESS);

        // Auto-start the server when the UI loads
        startServer();
    }

    // ── Toggle handler (called by FXML onAction) ──────────────────────────────
    @FXML
    private void toggleServer() {
        if (serverRunning) {
            stopServer();
        } else {
            startServer();
        }
    }

    private void startServer() {
        serverThread = new ServerThread(message -> {
            // Route Message objects to the log here however you need
            log(message.toString(), LogType.INFO);
        });
        serverThread.start();
        serverRunning = true;
        updateStatusUi(true);
        log("Server started.", LogType.SUCCESS);
    }

    private void stopServer() {
        if (serverThread != null) {
            serverThread.interrupt();
            serverThread = null;
        }
        serverRunning = false;
        updateStatusUi(false);
        log("Server stopped.", LogType.WARN);
    }

    // ── Update status dot, label, and button to reflect current state ─────────
    private void updateStatusUi(boolean running) {
        if (running) {
            statusDot.getStyleClass().setAll("status-dot-online");
            statusLabel.setText("SERVER RUNNING");
            statusLabel.setStyle("-fx-text-fill: #22c55e;");
            statusButton.setText("STOP");
            statusButton.getStyleClass().setAll("exec-button", "button-stop");
        } else {
            statusDot.getStyleClass().setAll("status-dot-offline");
            statusLabel.setText("SERVER OFFLINE");
            statusLabel.setStyle("-fx-text-fill: #ef4444;");
            statusButton.setText("START");
            statusButton.getStyleClass().setAll("exec-button", "button-start");
        }
    }

    // ── Rest of the class unchanged ───────────────────────────────────────────

    public static void log(String message, LogType type) {
        System.out.printf("[%s] [%s] %s%n",
                LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                type.name(), message);

        if (instance == null) return;
        final LogEntry entry = new LogEntry(LocalTime.now().format(STAMP_FMT), type, message);
        Platform.runLater(() -> instance.logItems.add(entry));
    }

    public static void log(String message) { log(message, LogType.INFO); }

    public static void setActiveGames(int n) {
        if (instance == null) return;
        Platform.runLater(() -> {
            instance.activeGames = n;
            instance.activeGamesLabel.setText(String.valueOf(n));
        });
    }

    public static void setTotalGames(int n) {
        if (instance == null) return;
        Platform.runLater(() -> {
            instance.totalGames = n;
            instance.totalGamesLabel.setText(String.valueOf(n));
        });
    }

    public static void setOnlineUsers(int n) {
        if (instance == null) return;
        Platform.runLater(() -> {
            instance.onlineUsers = n;
            instance.onlineUsersLabel.setText(String.valueOf(n));
            instance.clientCountLabel.setText(n + " client" + (n == 1 ? "" : "s"));
        });
    }

    private int activeGames = 0;
    private int totalGames  = 0;
    private int onlineUsers = 0;

    private void tick() {
        clockLabel.setText(LocalTime.now().format(TIME_FMT));
        uptimeLabel.setText(formatUptime());
    }

    private String formatUptime() {
        long totalSec = (System.currentTimeMillis() - startTimeMillis) / 1000;
        long h = totalSec / 3600;
        long m = (totalSec % 3600) / 60;
        long s = totalSec % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private static class LogCellFactory extends ListCell<LogEntry> {
        @Override
        protected void updateItem(LogEntry entry, boolean empty) {
            super.updateItem(entry, empty);
            if (empty || entry == null) { setGraphic(null); setText(null); return; }

            Label ts = new Label(entry.getTimestamp());
            ts.setStyle("-fx-text-fill: #374151; -fx-font-size: 11px; -fx-font-family: 'JetBrains Mono','Fira Code','Consolas',monospace;");
            ts.setMinWidth(110);

            Label badge = new Label(entry.getType().name());
            badge.setStyle(badgeStyle(entry.getType()));
            badge.setMinWidth(58);
            badge.setAlignment(Pos.CENTER);

            Label msg = new Label(entry.getMessage());
            msg.setStyle(messageStyle(entry.getType()));
            msg.setWrapText(false);
            HBox.setHgrow(msg, Priority.ALWAYS);

            HBox row = new HBox(10, ts, badge, msg);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: transparent;");

            setGraphic(row);
            setText(null);
            setStyle("-fx-background-color: transparent; -fx-padding: 1 0 1 0;");
        }

        private static String badgeStyle(LogType t) {
            String base = "-fx-font-size: 10px; -fx-font-family: 'JetBrains Mono','Fira Code','Consolas',monospace; -fx-background-radius: 3; -fx-padding: 1 6 1 6; -fx-font-weight: bold;";
            String colour;
            if      (t == LogType.SUCCESS) colour = "-fx-background-color: #14532d; -fx-text-fill: #4ade80;";
            else if (t == LogType.WARN)    colour = "-fx-background-color: #451a03; -fx-text-fill: #fbbf24;";
            else if (t == LogType.ERROR)   colour = "-fx-background-color: #450a0a; -fx-text-fill: #f87171;";
            else if (t == LogType.GAME)    colour = "-fx-background-color: #0c4a6e; -fx-text-fill: #22d3ee;";
            else if (t == LogType.CHAT)    colour = "-fx-background-color: #2e1065; -fx-text-fill: #a78bfa;";
            else                           colour = "-fx-background-color: #1e3a5f; -fx-text-fill: #60a5fa;";
            return base + colour;
        }

        private static String messageStyle(LogType t) {
            String base = "-fx-font-size: 13px; -fx-font-family: 'JetBrains Mono','Fira Code','Consolas',monospace;";
            String colour;
            if      (t == LogType.SUCCESS) colour = "-fx-text-fill: #4ade80;";
            else if (t == LogType.WARN)    colour = "-fx-text-fill: #fbbf24;";
            else if (t == LogType.ERROR)   colour = "-fx-text-fill: #f87171;";
            else if (t == LogType.GAME)    colour = "-fx-text-fill: #22d3ee;";
            else if (t == LogType.CHAT)    colour = "-fx-text-fill: #a78bfa;";
            else                           colour = "-fx-text-fill: #9ca3af;";
            return base + colour;
        }
    }
}
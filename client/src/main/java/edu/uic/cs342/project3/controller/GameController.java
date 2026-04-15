package edu.uic.cs342.project3.controller;

import edu.uic.cs342.project3.SceneManager;
import edu.uic.cs342.project3.http.ClientThread;
import edu.uic.cs342.project3.model.GameState;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Controller for game.fxml.
 * Calls ClientThread directly for all network operations.
 */
public class GameController {

    // ── Fields ────────────────────────────────────────────────────────────────

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @FXML private GridPane         boardGrid;
    @FXML private Label            turnLabel;
    @FXML private Label            player1Label;
    @FXML private Label            player2Label;
    @FXML private Label            player1SideLabel;
    @FXML private Label            player2SideLabel;
    @FXML private Label            playerColorLabel;
    @FXML private ListView<String> chatList;
    @FXML private TextField        chatField;
    @FXML private Button           sendButton;
    @FXML private VBox             chatBox;
    @FXML private Label            statusLabel;
    @FXML private VBox             gameOverOverlay;
    @FXML private Label            gameOverTitle;
    @FXML private Label            gameOverMessage;
    @FXML private Button           backToLobbyButton;

    private SceneManager sceneManager;
    private ClientThread client;
    private String       gameId;
    private String       username;
    private String       playerColor;

    private GameState currentState;
    private int       selectedRow = -1;
    private int       selectedCol = -1;

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "game-poller");
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
        gameOverOverlay.setVisible(false);
        gameOverOverlay.setManaged(false);
    }

    public void onEnter(String gameId, String username) {
        this.gameId       = gameId;
        this.username     = username;
        this.playerColor  = null;
        this.selectedRow  = -1;
        this.selectedCol  = -1;
        this.currentState = null;

        gameOverOverlay.setVisible(false);
        gameOverOverlay.setManaged(false);
        chatList.getItems().clear();
        statusLabel.setText("");

        loadGameState();
        startPolling();
    }

    @FXML
    private void handleSendMessage() {
        String message = chatField.getText().trim();
        if (message.isBlank()) return;
        chatField.clear();

        client.sendChatMessage(gameId, username, message,
            json -> loadGameState(),
            err  -> statusLabel.setText("Chat error: " + err));
    }

    @FXML
    private void handleBackToLobby() {
        stopPolling();
        playerColor  = null;
        currentState = null;
        sceneManager.showLobby(username);
    }

    private void loadGameState() {
        client.getGameState(gameId,
            json -> {
                try {
                    GameState state = MAPPER.treeToValue(json.get("gameState"), GameState.class);
                    applyState(state);
                } catch (Exception ex) {
                    statusLabel.setText("Error parsing state: " + ex.getMessage());
                }
            },
            err -> statusLabel.setText("Connection error: " + err));
    }

    private void applyState(GameState state) {
        this.currentState = state;

        if (playerColor == null && state.player1 != null) {
            playerColor = state.player1.equalsIgnoreCase(username) ? GameState.RED : GameState.BLACK;
            playerColorLabel.setText("You are: " + playerColor.toUpperCase());
            playerColorLabel.setStyle("-fx-text-fill: "
                    + (playerColor.equals(GameState.RED) ? "#f87171" : "#d1d5db") + ";");
        }

        String p1 = state.player1 + (state.player1.equalsIgnoreCase(username) ? " (you)" : "");
        String p2 = state.player2 + (state.player2.equalsIgnoreCase(username) ? " (you)" : "");
        player1Label.setText("\uD83D\uDD34 " + p1);
        player2Label.setText("\u26AB " + p2);
        if (player1SideLabel != null) player1SideLabel.setText(p1);
        if (player2SideLabel != null) player2SideLabel.setText(p2);

        if (state.isActive()) {
            boolean myTurn = state.currentTurn.equals(playerColor);
            turnLabel.setText(myTurn ? "Your Turn!" : "Opponent's Turn");
            turnLabel.setStyle(myTurn ? "-fx-text-fill: #4ade80;" : "-fx-text-fill: #9ca3af;");
        } else if (state.isCompleted()) {
            turnLabel.setText("Game Over");
            turnLabel.setStyle("-fx-text-fill: #fbbf24;");
        }

        renderBoard(state);
        refreshChat(state);

        chatBox.setVisible(!state.vsAI);
        chatBox.setManaged(!state.vsAI);

        if (state.isCompleted() && !gameOverOverlay.isVisible()) {
            stopPolling();
            showGameOver(state);
        }
    }

    private void renderBoard(GameState state) {
        boardGrid.getChildren().clear();
        boardGrid.getColumnConstraints().clear();
        boardGrid.getRowConstraints().clear();

        for (int i = 0; i < 8; i++) {
            boardGrid.getColumnConstraints().add(new ColumnConstraints(65));
            boardGrid.getRowConstraints().add(new RowConstraints(65));
        }

        for (int row = 0; row < 8; row++)
            for (int col = 0; col < 8; col++)
                boardGrid.add(buildCell(state, row, col), col, row);
    }

    private StackPane buildCell(GameState state, int row, int col) {
        StackPane cell = new StackPane();
        cell.setPrefSize(65, 65);

        boolean dark      = (row + col) % 2 == 1;
        boolean selected  = (row == selectedRow && col == selectedCol);
        boolean validMove = selectedRow >= 0 && state.board != null && state.board.grid != null
                && isValidMoveTarget(state, selectedRow, selectedCol, row, col);

        String bg;
        if      (selected)  bg = "#ca8a04";
        else if (validMove) bg = "#166534";
        else if (dark)      bg = "#1c1917";
        else                bg = "#d97706";
        cell.setStyle("-fx-background-color: " + bg + ";");

        if (validMove && state.board.grid[row][col] == null) {
            Circle dot = new Circle(8, Color.web("#4ade80"));
            dot.setOpacity(0.7);
            cell.getChildren().add(dot);
        }

        if (state.board != null && state.board.grid[row][col] != null)
            cell.getChildren().add(buildPiece(state.board.grid[row][col], selected));

        final int r = row, c = col;
        cell.setOnMouseClicked(e -> handleCellClick(r, c));
        return cell;
    }

    private StackPane buildPiece(GameState.Piece piece, boolean selected) {
        StackPane sp     = new StackPane();
        boolean   isRed  = GameState.RED.equals(piece.color);
        Circle    circle = new Circle(26);
        circle.setFill(Color.web(isRed ? "#dc2626" : "#374151"));
        circle.setStroke(Color.web(isRed ? "#7f1d1d" : "#111827"));
        circle.setStrokeWidth(3);
        if (selected) circle.setEffect(new DropShadow(10, Color.YELLOW));
        sp.getChildren().add(circle);

        if (piece.king) {
            Text crown = new Text("\u265B");
            crown.setFill(Color.web("#fbbf24"));
            crown.setStyle("-fx-font-size: 18px;");
            sp.getChildren().add(crown);
        }
        return sp;
    }

    private void handleCellClick(int row, int col) {
        if (currentState == null || !currentState.isActive()) return;
        if (!currentState.currentTurn.equals(playerColor)) {
            statusLabel.setText("It's not your turn!");
            return;
        }

        GameState.Piece piece = currentState.board.grid[row][col];

        if (piece != null && piece.color.equals(playerColor)) {
            selectedRow = row;
            selectedCol = col;
            statusLabel.setText("Piece selected. Choose a destination.");
            renderBoard(currentState);
            return;
        }

        if (selectedRow >= 0 && piece == null) {
            attemptMove(selectedRow, selectedCol, row, col);
            selectedRow = -1;
            selectedCol = -1;
            return;
        }

        statusLabel.setText("Select one of your pieces first.");
    }

    private void attemptMove(int fromRow, int fromCol, int toRow, int toCol) {
        statusLabel.setText("Moving...");
        client.makeMove(gameId, fromRow, fromCol, toRow, toCol, username,
            json -> {
                statusLabel.setText("");
                loadGameState();
            },
            err -> {
                statusLabel.setText("Invalid move: " + err);
                loadGameState();
            });
    }

    private boolean isValidMoveTarget(GameState state,
                                      int fromRow, int fromCol,
                                      int toRow,   int toCol) {
        if (state.board == null) return false;
        GameState.Piece piece = state.board.grid[fromRow][fromCol];
        if (piece == null || !piece.color.equals(playerColor)) return false;

        int dr = toRow - fromRow;
        int dc = toCol - fromCol;

        if (Math.abs(dr) == 1 && Math.abs(dc) == 1) {
            boolean forward = piece.color.equals(GameState.RED) ? dr < 0 : dr > 0;
            return (forward || piece.king) && state.board.grid[toRow][toCol] == null;
        }

        if (Math.abs(dr) == 2 && Math.abs(dc) == 2) {
            int mr = fromRow + dr / 2, mc = fromCol + dc / 2;
            if (mr < 0 || mr >= 8 || mc < 0 || mc >= 8) return false;
            GameState.Piece mid     = state.board.grid[mr][mc];
            boolean         forward = piece.color.equals(GameState.RED) ? dr < 0 : dr > 0;
            return (forward || piece.king)
                    && mid != null
                    && !mid.color.equals(playerColor)
                    && state.board.grid[toRow][toCol] == null;
        }

        return false;
    }

    private void refreshChat(GameState state) {
        chatList.getItems().clear();
        if (state.chat != null) {
            for (GameState.ChatEntry msg : state.chat)
                chatList.getItems().add("[" + msg.player + "] " + msg.message);
            if (!chatList.getItems().isEmpty())
                chatList.scrollTo(chatList.getItems().size() - 1);
        }
    }

    private void showGameOver(GameState state) {
        gameOverTitle.setText("Game Over!");
        if ("draw".equals(state.winner)) {
            gameOverMessage.setText("\uD83E\uDD1D It's a draw!");
            gameOverMessage.setStyle("-fx-text-fill: #fbbf24; -fx-font-weight: bold;");
        } else if (state.winner != null && state.winner.equals(playerColor)) {
            gameOverMessage.setText("\uD83C\uDFC6 You won!");
            gameOverMessage.setStyle("-fx-text-fill: #4ade80; -fx-font-weight: bold;");
        } else {
            gameOverMessage.setText("\uD83D\uDC94 You lost. Better luck next time!");
            gameOverMessage.setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold;");
        }
        gameOverOverlay.setVisible(true);
        gameOverOverlay.setManaged(true);
    }

    private void startPolling() {
        stopPolling();
        pollFuture = scheduler.scheduleAtFixedRate(this::loadGameState, 2, 2, TimeUnit.SECONDS);
    }

    private void stopPolling() {
        if (pollFuture != null) { pollFuture.cancel(false); pollFuture = null; }
    }
}

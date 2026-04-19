package edu.uic.cs342.controller;

import edu.uic.cs342.SceneManager;
import edu.uic.cs342.http.ClientThread;
import edu.uic.cs342.model.GameState;
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
        this.gameOverOverlay.setVisible(false);
        this.gameOverOverlay.setManaged(false);
    }

    public void onEnter(String gameId, String username) {
        this.gameId       = gameId;
        this.username     = username;
        this.playerColor  = null;
        this.selectedRow  = -1;
        this.selectedCol  = -1;
        this.currentState = null;

        this.gameOverOverlay.setVisible(false);
        this.gameOverOverlay.setManaged(false);
        this.chatList.getItems().clear();
        this.statusLabel.setText("");

        this.loadGameState();
        this.startPolling();
    }

    @FXML
    private void handleSendMessage() {
        String message = this.chatField.getText().trim();
        if (message.isBlank()) return;
        this.chatField.clear();

        this.client.sendChatMessage(this.gameId, this.username, message,
            json -> this.loadGameState(),
            err  -> this.statusLabel.setText("Chat error: " + err));
    }

    @FXML
    private void handleBackToLobby() {
        this.stopPolling();
        this.playerColor  = null;
        this.currentState = null;
        this.sceneManager.showLobby(this.username);
    }

    private void loadGameState() {
        this.client.getGameState(this.gameId,
            json -> {
                try {
                    GameState state = GameController.MAPPER.treeToValue(json.get("gameState"), GameState.class);
                    this.applyState(state);
                } catch (Exception ex) {
                    this.statusLabel.setText("Error parsing state: " + ex.getMessage());
                }
            },
            err -> this.statusLabel.setText("Connection error: " + err));
    }

    private void applyState(GameState state) {
        this.currentState = state;

        if (this.playerColor == null && state.player1 != null) {
            this.playerColor = state.player1.equalsIgnoreCase(this.username) ? GameState.RED : GameState.BLACK;
            this.playerColorLabel.setText("You are: " + this.playerColor.toUpperCase());
            this.playerColorLabel.setStyle("-fx-text-fill: "
                    + (this.playerColor.equals(GameState.RED) ? "#f87171" : "#d1d5db") + ";");
        }

        String p1 = state.player1 + (state.player1.equalsIgnoreCase(this.username) ? " (you)" : "");
        String p2 = state.player2 + (state.player2.equalsIgnoreCase(this.username) ? " (you)" : "");
        this.player1Label.setText("\uD83D\uDD34 " + p1);
        this.player2Label.setText("\u26AB " + p2);
        if (this.player1SideLabel != null) this.player1SideLabel.setText(p1);
        if (this.player2SideLabel != null) this.player2SideLabel.setText(p2);

        if (state.isActive()) {
            boolean myTurn = state.currentTurn.equals(this.playerColor);
            this.turnLabel.setText(myTurn ? "Your Turn!" : "Opponent's Turn");
            this.turnLabel.setStyle(myTurn ? "-fx-text-fill: #4ade80;" : "-fx-text-fill: #9ca3af;");
        } else if (state.isCompleted()) {
            this.turnLabel.setText("Game Over");
            this.turnLabel.setStyle("-fx-text-fill: #fbbf24;");
        }

        this.renderBoard(state);
        this.refreshChat(state);

        this.chatBox.setVisible(!state.vsAI);
        this.chatBox.setManaged(!state.vsAI);

        if (state.isCompleted() && !this.gameOverOverlay.isVisible()) {
            this.stopPolling();
            this.showGameOver(state);
        }
    }

    private void renderBoard(GameState state) {
        this.boardGrid.getChildren().clear();
        this.boardGrid.getColumnConstraints().clear();
        this.boardGrid.getRowConstraints().clear();

        for (int i = 0; i < 8; i++) {
            this.boardGrid.getColumnConstraints().add(new ColumnConstraints(65));
            this.boardGrid.getRowConstraints().add(new RowConstraints(65));
        }

        for (int row = 0; row < 8; row++)
            for (int col = 0; col < 8; col++)
                this.boardGrid.add(this.buildCell(state, row, col), col, row);
    }

    private StackPane buildCell(GameState state, int row, int col) {
        StackPane cell = new StackPane();
        cell.setPrefSize(65, 65);

        boolean dark      = (row + col) % 2 == 1;
        boolean selected  = (row == this.selectedRow && col == this.selectedCol);
        boolean validMove = this.selectedRow >= 0 && state.board != null && state.board.grid != null
                && this.isValidMoveTarget(state, this.selectedRow, this.selectedCol, row, col);

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
            cell.getChildren().add(this.buildPiece(state.board.grid[row][col], selected));

        final int r = row, c = col;
        cell.setOnMouseClicked(e -> this.handleCellClick(r, c));
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
        if (this.currentState == null || !this.currentState.isActive()) return;
        if (!this.currentState.currentTurn.equals(this.playerColor)) {
            this.statusLabel.setText("It's not your turn!");
            return;
        }

        GameState.Piece piece = this.currentState.board.grid[row][col];

        if (piece != null && piece.color.equals(this.playerColor)) {
            this.selectedRow = row;
            this.selectedCol = col;
            this.statusLabel.setText("Piece selected. Choose a destination.");
            this.renderBoard(this.currentState);
            return;
        }

        if (this.selectedRow >= 0 && piece == null) {
            this.attemptMove(this.selectedRow, this.selectedCol, row, col);
            this.selectedRow = -1;
            this.selectedCol = -1;
            return;
        }

        this.statusLabel.setText("Select one of your pieces first.");
    }

    private void attemptMove(int fromRow, int fromCol, int toRow, int toCol) {
        this.statusLabel.setText("Moving...");
        this.client.makeMove(this.gameId, fromRow, fromCol, toRow, toCol, this.username,
            json -> {
                this.statusLabel.setText("");
                this.loadGameState();
            },
            err -> {
                this.statusLabel.setText("Invalid move: " + err);
                this.loadGameState();
            });
    }

    private boolean isValidMoveTarget(GameState state,
                                      int fromRow, int fromCol,
                                      int toRow,   int toCol) {
        if (state.board == null || state.board.grid == null) return false;
        GameState.Piece[][] grid  = state.board.grid;
        GameState.Piece     piece = grid[fromRow][fromCol];
        if (piece == null || !piece.color.equals(this.playerColor)) return false;

        int dr = toRow - fromRow;
        int dc = toCol - fromCol;

        boolean isJump    = Math.abs(dr) == 2 && Math.abs(dc) == 2;
        boolean isRegular = Math.abs(dr) == 1 && Math.abs(dc) == 1;

        if (!isJump && !isRegular) return false;

        boolean anyJumpExists = this.anyJumpExists(grid, this.playerColor);
        if (anyJumpExists && !isJump) return false;

        boolean forward = piece.color.equals(GameState.RED) ? dr < 0 : dr > 0;

        if (isRegular) {
            return (forward || piece.king) && grid[toRow][toCol] == null;
        }

        int mr = fromRow + dr / 2, mc = fromCol + dc / 2;
        if (mr < 0 || mr >= 8 || mc < 0 || mc >= 8) return false;
        GameState.Piece mid = grid[mr][mc];
        return (forward || piece.king)
                && mid != null
                && !mid.color.equals(this.playerColor)
                && grid[toRow][toCol] == null;
    }

    private boolean anyJumpExists(GameState.Piece[][] grid, String color) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                GameState.Piece p = grid[r][c];
                if (p == null || !p.color.equals(color)) continue;
                if (this.pieceHasJump(grid, r, c, p)) return true;
            }
        }
        return false;
    }

    private boolean pieceHasJump(GameState.Piece[][] grid, int r, int c, GameState.Piece p) {
        int[][] dirs;
        if (p.king) {
            dirs = new int[][]{{-1,-1},{-1,1},{1,-1},{1,1}};
        } else if (p.color.equals(GameState.RED)) {
            dirs = new int[][]{{-1,-1},{-1,1}};
        } else {
            dirs = new int[][]{{1,-1},{1,1}};
        }

        for (int[] d : dirs) {
            int mr = r + d[0], mc = c + d[1];
            int nr = r + d[0] * 2, nc = c + d[1] * 2;
            if (nr < 0 || nr >= 8 || nc < 0 || nc >= 8) continue;
            GameState.Piece mid = grid[mr][mc];
            if (mid != null && !mid.color.equals(p.color) && grid[nr][nc] == null)
                return true;
        }
        return false;
    }

    private void refreshChat(GameState state) {
        this.chatList.getItems().clear();
        if (state.chat != null) {
            for (GameState.ChatEntry msg : state.chat)
                this.chatList.getItems().add("[" + msg.player + "] " + msg.message);
            if (!this.chatList.getItems().isEmpty())
                this.chatList.scrollTo(this.chatList.getItems().size() - 1);
        }
    }

    private void showGameOver(GameState state) {
        this.gameOverTitle.setText("Game Over!");
        if ("draw".equals(state.winner)) {
            this.gameOverMessage.setText("\uD83E\uDD1D It's a draw!");
            this.gameOverMessage.setStyle("-fx-text-fill: #fbbf24; -fx-font-weight: bold;");
        } else if (state.winner != null && state.winner.equals(this.playerColor)) {
            this.gameOverMessage.setText("\uD83C\uDFC6 You won!");
            this.gameOverMessage.setStyle("-fx-text-fill: #4ade80; -fx-font-weight: bold;");
        } else {
            this.gameOverMessage.setText("\uD83D\uDC94 You lost. Better luck next time!");
            this.gameOverMessage.setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold;");
        }
        this.gameOverOverlay.setVisible(true);
        this.gameOverOverlay.setManaged(true);
    }

    private void startPolling() {
        this.stopPolling();
        this.pollFuture = this.scheduler.scheduleAtFixedRate(this::loadGameState, 2, 2, TimeUnit.SECONDS);
    }

    private void stopPolling() {
        if (this.pollFuture != null) { this.pollFuture.cancel(false); this.pollFuture = null; }
    }
}

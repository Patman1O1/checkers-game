package edu.uic.cs342.project3.controllers;

import edu.uic.cs342.project3.SceneManager;
import edu.uic.cs342.project3.http.ClientThread;
import edu.uic.cs342.project3.models.GameState;

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

import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class GameController {
    // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────────
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ScheduledExecutorService scheduler;

    @FXML
    private GridPane boardGrid;

    @FXML
    private Label turnLabel;

    @FXML
    private Label player1Label;

    @FXML
    private Label player2Label;

    @FXML
    private Label player1SideLabel;

    @FXML
    private Label player2SideLabel;

    @FXML
    private Label playerColorLabel;

    @FXML
    private ListView<String> chatList;

    @FXML
    private TextField chatField;

    @FXML
    private Button sendButton;

    @FXML
    private VBox chatBox;

    @FXML
    private Label statusLabel;

    @FXML
    private VBox gameOverOverlay;

    @FXML
    private Label gameOverTitle;

    @FXML
    private Label gameOverMessage;

    @FXML
    private Button backToLobbyButton;

    private SceneManager sceneManager;

    private ClientThread client;

    private String gameId, username, playerColor;

    private GameState currentState;

    private int selectedRow = -1, selectedCol = -1;

    private ScheduledFuture<?> pollFuture;

    public static final URL SCENE_FXML = GameController.class.getResource("/fxml/game.fxml");

    public static double SCENE_WIDTH = 1200.0;

    public static double SCENE_HEIGHT = 760.0;

    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────
    public GameController() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "game-poller");
            thread.setDaemon(true);
            return thread;
        });
    }

    // ── Setters ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public void setSceneManager(SceneManager sceneManager) { this.sceneManager = sceneManager; }

    public void setClientThread(ClientThread clientThread) { this.client = clientThread; }

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    @FXML
    private void handleSendMessage() {
        String message = this.chatField.getText().trim();
        if (message.isBlank()) {
            return;
        }
        this.chatField.clear();

        this.client.sendChatMessage(this.gameId, this.username, message,
                json -> this.loadGameState(),
                error -> this.statusLabel.setText(String.format("Chat error: %s", error)));
    }

    @FXML
    private void handleBackToLobby() {
        this.stopPolling();
        this.playerColor = null;
        this.currentState = null;
        this.sceneManager.showLobby(this.username);
    }

    private void loadGameState() {
        this.client.getGameState(this.gameId,
                json -> {
                    try {
                        GameState state = GameController.OBJECT_MAPPER.treeToValue(json.get("gameState"), GameState.class);
                        this.applyState(state);
                    } catch (Exception exception) {
                        this.statusLabel.setText(String.format("Error parsing state: %s", exception.getMessage()));
                    }
                },
                error -> this.statusLabel.setText(String.format("Connection error: %s", error)));
    }

    private void applyState(GameState state) {
        this.currentState = state;

        if (this.playerColor == null && state.player1 != null) {
            this.playerColor = state.player1.equalsIgnoreCase(this.username) ? GameState.RED : GameState.BLACK;
            this.playerColorLabel.setText(String.format("You are: %s", this.playerColor.toUpperCase()));
            if (this.playerColor.equals(GameState.RED)) {
                this.playerColorLabel.setStyle("-fx-text-fill: #f87171;");
            } else {
                this.playerColorLabel.setStyle("-fx-text-fill: #d1d5db;");
            }
        }

        // Set player 1's name
        String player1Name = state.player1;
        if (state.player1.equalsIgnoreCase(this.username)) {
            player1Name = player1Name.concat("(you)");
        }
        this.player1Label.setText(String.format("\uD83D\uDD34 %s", player1Name));
        if (this.player1SideLabel != null) {
            this.player1SideLabel.setText(player1Name);
        }

        // Set player 2's name
        String player2Name = state.player2;
        if (state.player2.equalsIgnoreCase(this.username)) {
            player2Name = player2Name.concat("(you)");
        }
        this.player2Label.setText(String.format("\u26AB %s", player2Name));
        if (this.player1SideLabel != null) {
            this.player1SideLabel.setText(player2Name);
        }

        if (state.isActive()) {
            if (state.currentTurn.equals(this.playerColor)) {
                this.turnLabel.setText("Your Turn!");
                this.turnLabel.setStyle("-fx-text-fill: #4ade80;");
            } else {
                this.turnLabel.setText("Opponent's Turn");
                this.turnLabel.setStyle("-fx-text-fill: #9ca3af;");
            }
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

        for (int i = 0; i < 8; ++i) {
            this.boardGrid.getColumnConstraints().add(new ColumnConstraints(65.0));
            this.boardGrid.getRowConstraints().add(new RowConstraints(65.0));
        }

        for (int rowNum = 0; rowNum < 8; ++rowNum) {
            for (int colNum = 0; colNum < 8; ++colNum) {
                this.boardGrid.add(this.buildCell(state, rowNum, colNum), colNum, rowNum);
            }
        }
    }

    private StackPane buildCell(GameState state, int rowNum, int colNum) {
        StackPane cell = new StackPane();
        cell.setPrefSize(65.0, 65.0);

        boolean isSelected = (rowNum == this.selectedRow && colNum == this.selectedCol);
        boolean isValidMove = this.selectedRow >= 0 && state.board != null && state.board.grid != null
                && this.isValidMoveTarget(state, this.selectedRow, this.selectedCol, rowNum, colNum);

        // Set the square color
        if (isSelected) {
            cell.setStyle("-fx-background-color: #ca8a04;");
        } else if (isValidMove) {
            cell.setStyle("-fx-background-color: #166534;");
        } else if ((rowNum + colNum) % 2 == 1) {
            cell.setStyle("-fx-background-color: #1c1917;");
        } else {
            cell.setStyle("-fx-background-color: #d97706;");
        }

        if (isValidMove && state.board.grid[rowNum][colNum] == null) {
            Circle dot = new Circle(8.0, Color.web("#4ade80"));
            dot.setOpacity(0.7);
            cell.getChildren().add(dot);
        }

        if (state.board != null && state.board.grid[rowNum][colNum] != null) {
            cell.getChildren().add(this.buildPiece(state.board.grid[rowNum][colNum], isSelected));
        }

        cell.setOnMouseClicked(event -> this.handleCellClick(rowNum, colNum));
        return cell;
    }

    private StackPane buildPiece(GameState.Piece piece, boolean isSelected) {
        StackPane stackPane = new StackPane();

        Circle circle = new Circle(26.0);
        if (GameState.RED.equals(piece.color)) {
            circle.setFill(Color.web("#dc2626"));
            circle.setStroke(Color.web("#7f1d1d"));
        } else {
            circle.setFill(Color.web("#374151"));
            circle.setStroke(Color.web("#111827"));
        }
        circle.setStrokeWidth(3.0);

        if (isSelected) {
            circle.setEffect(new DropShadow(10.0, Color.YELLOW));
        }
        stackPane.getChildren().add(circle);

        if (piece.king) {
            Text crown = new Text("\u265B");
            crown.setFill(Color.web("#fbbf24"));
            crown.setStyle("-fx-font-size: 18px;");
            stackPane.getChildren().add(crown);
        }
        return stackPane;
    }

    private void handleCellClick(int rowNum, int colNum) {
        if (this.currentState == null || !this.currentState.isActive()) return;
        if (!this.currentState.currentTurn.equals(this.playerColor)) {
            this.statusLabel.setText("It's not your turn!");
            return;
        }

        GameState.Piece piece = this.currentState.board.grid[rowNum][colNum];

        if (piece != null && piece.color.equals(this.playerColor)) {
            this.selectedRow = rowNum;
            this.selectedCol = colNum;
            this.statusLabel.setText("Piece selected. Choose a destination.");
            this.renderBoard(this.currentState);
            return;
        }

        if (this.selectedRow >= 0 && piece == null) {
            this.attemptMove(this.selectedRow, this.selectedCol, rowNum, colNum);
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
                error -> {
                    this.statusLabel.setText(String.format("Invalid move: %s", error));
                    this.loadGameState();
                });
    }

    private boolean isValidMoveTarget(GameState state, int fromRow, int fromCol, int toRow, int toCol) {
        if (state.board == null || state.board.grid == null) {
            return false;
        }
        GameState.Piece[][] grid  = state.board.grid;
        GameState.Piece piece = grid[fromRow][fromCol];
        if (piece == null || !piece.color.equals(this.playerColor)) {
            return false;
        }

        int rowDist = toRow - fromRow;
        int colDist = toCol - fromCol;

        boolean isJump = Math.abs(rowDist) == 2 && Math.abs(colDist) == 2;
        boolean isRegular = Math.abs(rowDist) == 1 && Math.abs(colDist) == 1;

        if (!isJump && !isRegular) {
            return false;
        }
        if (!isJump && this.anyJumpExists(grid, this.playerColor)) {
            return false;
        }

        boolean isForward = piece.color.equals(GameState.RED) ? rowDist < 0 : rowDist > 0;

        if (isRegular) {
            return (isForward || piece.king) && grid[toRow][toCol] == null;
        }

        int middleRow = fromRow + rowDist / 2;
        int middleCol = fromCol + colDist / 2;

        if (middleRow < 0 || middleRow >= 8 || middleCol < 0 || middleCol >= 8) {
            return false;
        }
        GameState.Piece middlePiece = grid[middleRow][middleCol];
        return (isForward || piece.king)
                && middlePiece != null
                && !middlePiece.color.equals(this.playerColor)
                && grid[toRow][toCol] == null;
    }

    private boolean anyJumpExists(GameState.Piece[][] grid, String color) {
        for (int rowNum = 0; rowNum < 8; ++rowNum) {
            for (int colNum = 0; colNum < 8; ++colNum) {
                GameState.Piece piece = grid[rowNum][colNum];
                if (piece == null || !piece.color.equals(color)) {
                    continue;
                }

                if (this.pieceHasJump(grid, rowNum, colNum, piece)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean pieceHasJump(GameState.Piece[][] grid, int rowNum, int colNum, GameState.Piece piece) {
        int[][] directions;
        if (piece.king) {
            directions = new int[][]{{-1,-1},{-1,1},{1,-1},{1,1}};
        } else if (piece.color.equals(GameState.RED)) {
            directions = new int[][]{{-1,-1},{-1,1}};
        } else {
            directions = new int[][]{{1,-1},{1,1}};
        }

        for (int[] direction : directions) {
            int middleRow = rowNum + direction[0];
            int middleCol = colNum + direction[1];

            int nextRow = rowNum + direction[0] * 2;
            int nextCol = colNum + direction[1] * 2;

            if (nextRow < 0 || nextRow >= 8 || nextCol < 0 || nextCol >= 8) {
                continue;
            }

            GameState.Piece middlePiece = grid[middleRow][middleCol];
            if (middlePiece != null && !middlePiece.color.equals(piece.color) && grid[nextRow][nextCol] == null) {
                return true;
            }
        }
        return false;
    }

    private void refreshChat(GameState state) {
        this.chatList.getItems().clear();
        if (state.chat != null) {
            for (GameState.ChatEntry chatEntry : state.chat) {
                this.chatList.getItems().add(String.format("[%s] %s", chatEntry.player, chatEntry.message));
            }

            if (!this.chatList.getItems().isEmpty()) {
                this.chatList.scrollTo(this.chatList.getItems().size() - 1);
            }
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
        this.pollFuture = this.scheduler.scheduleAtFixedRate(this::loadGameState, 2L, 2L, TimeUnit.SECONDS);
    }

    private void stopPolling() {
        if (this.pollFuture != null) {
            this.pollFuture.cancel(false);
            this.pollFuture = null;
        }
    }

    @FXML
    public void initialize() {
        this.gameOverOverlay.setVisible(false);
        this.gameOverOverlay.setManaged(false);
    }

    public void onEnter(String gameId, String username) {
        this.gameId = gameId;
        this.username = username;
        this.playerColor = null;
        this.selectedRow = -1;
        this.selectedCol = -1;
        this.currentState = null;

        this.gameOverOverlay.setVisible(false);
        this.gameOverOverlay.setManaged(false);
        this.chatList.getItems().clear();
        this.statusLabel.setText("");

        this.loadGameState();
        this.startPolling();
    }
}
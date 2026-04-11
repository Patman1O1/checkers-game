package edu.uic.cs342.project3.frontend;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class SceneManager {
    // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────────
    private static final URL LOGIN_FXML = SceneManager.class.getResource("/fxml/login.fxml");

    private static final URL LOBBY_FXML = SceneManager.class.getResource("/fxml/lobby.fxml");

    private static final URL GAME_FXML = SceneManager.class.getResource("/fxml/game.fxml");

    private static final String CSS = Objects.requireNonNull(SceneManager.class.getResource("/css/index.css")).toExternalForm();

    private static volatile SceneManager instance = null;

    private final Scene loginScene, lobbyScene, gameScene;

    private Stage primaryStage;

    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────
    private SceneManager() throws IOException {
        this.loginScene = new Scene(new FXMLLoader(SceneManager.LOGIN_FXML).load());
        this.loginScene.getStylesheets().add(SceneManager.CSS);

        this.lobbyScene = new Scene(new FXMLLoader(SceneManager.LOBBY_FXML).load());
        this.lobbyScene.getStylesheets().add(SceneManager.CSS);

        this.gameScene = new Scene(new FXMLLoader(SceneManager.GAME_FXML).load());
        this.gameScene.getStylesheets().add(SceneManager.CSS);

        this.primaryStage = null;
    }

    // ── Setters ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public void setPrimaryStage(Stage primaryStage) throws NullPointerException {
        if (primaryStage == null) {
            throw new NullPointerException("primaryStage is null");
        }
        this.primaryStage = primaryStage;
    }

    // ── Getters ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public static synchronized SceneManager getInstance() throws IOException {
        if (SceneManager.instance == null) {
            SceneManager.instance = new SceneManager();
        }
        return SceneManager.instance;
    }

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public void showLoginScene() throws NullPointerException {
        if (this.primaryStage == null) {
            throw new NullPointerException("primaryStage is null");
        }
        this.primaryStage.setScene(this.loginScene);
        this.primaryStage.sizeToScene();
    }

    public void showLobbyScene() throws NullPointerException {
        if (this.primaryStage == null) {
            throw new NullPointerException("primaryStage is null");
        }
        this.primaryStage.setScene(this.lobbyScene);
        this.primaryStage.sizeToScene();
    }

    public void showGameScene() throws NullPointerException {
        if (this.primaryStage == null) {
            throw new NullPointerException("primaryStage is null");
        }
        this.primaryStage.setScene(this.gameScene);
        this.primaryStage.sizeToScene();
    }
}

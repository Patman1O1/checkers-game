package edu.uic.cs342.project3;

import edu.uic.cs342.project3.controllers.GameController;
import edu.uic.cs342.project3.controllers.LobbyController;
import edu.uic.cs342.project3.controllers.LoginController;
import edu.uic.cs342.project3.http.ClientThread;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class SceneManager {
    // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────────
    private static final String CSS = Objects.requireNonNull(
            Objects.requireNonNull(SceneManager.class.getResource("/css/index.css")).toExternalForm()
    );

    private static SceneManager instance;

    private final Stage primaryStage;

    private final ClientThread clientThread;

    private final Scene loginScene, lobbyScene, gameScene;

    private final FXMLLoader loginLoader, lobbyLoader, gameLoader;

    private String currentUsername, currentGameId;

    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────
    private SceneManager(Stage primaryStage, ClientThread clientThread) throws IOException {
        this.primaryStage = primaryStage;
        this.clientThread = clientThread;

        // Load the login scene
        this.loginLoader = new FXMLLoader(LoginController.SCENE_FXML);
        this.loginScene = new Scene(this.loginLoader.load(), LoginController.SCENE_WIDTH, LoginController.SCENE_HEIGHT);
        this.loginScene.getStylesheets().add(SceneManager.CSS);

        // Load the lobby scene
        this.lobbyLoader = new FXMLLoader(LobbyController.SCENE_FXML);
        this.lobbyScene = new Scene(this.lobbyLoader.load(), LobbyController.SCENE_WIDTH, LobbyController.SCENE_HEIGHT);
        this.lobbyScene.getStylesheets().add(SceneManager.CSS);

        // Load the game scene
        this.gameLoader = new FXMLLoader(GameController.SCENE_FXML);
        this.gameScene = new Scene(this.gameLoader.load(), GameController.SCENE_WIDTH, GameController.SCENE_HEIGHT);
        this.gameScene.getStylesheets().add(SceneManager.CSS);

        LoginController loginController = this.loginLoader.getController();
        loginController.setSceneManager(this);
        loginController.setClientThread(this.clientThread);

        LobbyController lobbyController = this.lobbyLoader.getController();
        lobbyController.setSceneManager(this);
        lobbyController.setClientThread(this.clientThread);

        GameController gameController = this.gameLoader.getController();
        gameController.setSceneManager(this);
        gameController.setClientThread(this.clientThread);
    }

    // ── Getters ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public static SceneManager getInstance() throws IllegalStateException {
        if (SceneManager.instance == null) {
            throw new IllegalStateException("SceneManager not yet initialised");
        }
        return SceneManager.instance;
    }

    public String getCurrentUsername() { return this.currentUsername; }

    public String getCurrentGameId() { return this.currentGameId; }

    public Stage getPrimaryStage() { return this.primaryStage; }

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public static SceneManager create(Stage stage, ClientThread clientThread) throws Exception {
        SceneManager sceneManager = new SceneManager(stage, clientThread);
        SceneManager.instance = sceneManager;
        return sceneManager;
    }

    public void showLogin() {
        ((LoginController) this.loginLoader.getController()).reset();

        this.primaryStage.setTitle("Checkers Online \u2013 Login");
        this.primaryStage.setScene(this.loginScene);
        this.primaryStage.show();
    }

    public void showLobby(String username) {
        this.currentUsername = username;

        ((LobbyController) this.lobbyLoader.getController()).onEnter();

        this.primaryStage.setTitle(String.format("Checkers Online \u2013 Lobby  [%s]", username));
        this.primaryStage.setScene(this.lobbyScene);
    }

    public void showGame(String gameId) {
        this.currentGameId = gameId;

        ((GameController) this.gameLoader.getController()).onEnter(gameId, this.currentUsername);

        this.primaryStage.setTitle("Checkers Online \u2013 Game");
        this.primaryStage.setScene(this.gameScene);
    }
}

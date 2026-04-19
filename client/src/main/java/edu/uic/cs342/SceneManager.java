package edu.uic.cs342;

import edu.uic.cs342.controller.GameController;
import edu.uic.cs342.controller.LobbyController;
import edu.uic.cs342.controller.LoginController;
import edu.uic.cs342.http.ClientThread;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.logging.Logger;

public class SceneManager {

    // ── Fields ────────────────────────────────────────────────────────────────

    private static final Logger       LOG      = Logger.getLogger(SceneManager.class.getName());
    private static       SceneManager instance;

    private final Stage        stage;
    private final ClientThread client;
    private       Scene        loginScene;
    private       Scene        lobbyScene;
    private       Scene        gameScene;

    private FXMLLoader loginLoader;
    private FXMLLoader lobbyLoader;
    private FXMLLoader gameLoader;

    private String currentUsername;
    private String currentGameId;

    // ── Constructors ──────────────────────────────────────────────────────────

    private SceneManager(Stage stage, ClientThread client) throws Exception {
        this.stage  = stage;
        this.client = client;
        this.preloadScenes();
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public static SceneManager getInstance() {
        if (SceneManager.instance == null)
            throw new IllegalStateException("SceneManager not yet initialised");
        return SceneManager.instance;
    }

    public String getCurrentUsername() { return this.currentUsername; }
    public String getCurrentGameId()   { return this.currentGameId;   }
    public Stage  getStage()           { return this.stage;           }

    // ── Methods ───────────────────────────────────────────────────────────────

    public static SceneManager create(Stage stage, ClientThread client) throws Exception {
        SceneManager sm = new SceneManager(stage, client);
        SceneManager.instance = sm;
        return sm;
    }

    public void showLogin() {
        LoginController lc = this.loginLoader.getController();
        lc.reset();
        this.stage.setTitle("Checkers Online \u2013 Login");
        this.stage.setScene(this.loginScene);
        this.stage.show();
    }

    public void showLobby(String username) {
        this.currentUsername = username;
        LobbyController lob = this.lobbyLoader.getController();
        lob.onEnter();
        this.stage.setTitle("Checkers Online \u2013 Lobby  [" + username + "]");
        this.stage.setScene(this.lobbyScene);
    }

    public void showGame(String gameId) {
        this.currentGameId = gameId;
        GameController gc = this.gameLoader.getController();
        gc.onEnter(gameId, this.currentUsername);
        this.stage.setTitle("Checkers Online \u2013 Game");
        this.stage.setScene(this.gameScene);
    }

    private void preloadScenes() throws Exception {
        this.loginLoader = new FXMLLoader(this.getClass().getResource("/fxml/login.fxml"));
        Parent loginRoot = this.loginLoader.load();
        this.loginScene = new Scene(loginRoot, 480, 560);
        this.loginScene.getStylesheets().add(this.css("dark-theme"));

        this.lobbyLoader = new FXMLLoader(this.getClass().getResource("/fxml/lobby.fxml"));
        Parent lobbyRoot = this.lobbyLoader.load();
        this.lobbyScene = new Scene(lobbyRoot, 1100, 700);
        this.lobbyScene.getStylesheets().add(this.css("dark-theme"));

        this.gameLoader = new FXMLLoader(this.getClass().getResource("/fxml/game.fxml"));
        Parent gameRoot = this.gameLoader.load();
        this.gameScene = new Scene(gameRoot, 1200, 760);
        this.gameScene.getStylesheets().add(this.css("dark-theme"));

        LoginController lc  = this.loginLoader.getController();
        LobbyController lob = this.lobbyLoader.getController();
        GameController  gc  = this.gameLoader.getController();

        lc.setSceneManager(this);  lc.setClientThread(this.client);
        lob.setSceneManager(this); lob.setClientThread(this.client);
        gc.setSceneManager(this);  gc.setClientThread(this.client);
    }

    private String css(String name) {
        return this.getClass().getResource("/css/" + name + ".css").toExternalForm();
    }
}

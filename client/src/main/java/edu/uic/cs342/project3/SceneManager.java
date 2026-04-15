package edu.uic.cs342.project3;

import edu.uic.cs342.project3.controller.GameController;
import edu.uic.cs342.project3.controller.LobbyController;
import edu.uic.cs342.project3.controller.LoginController;
import edu.uic.cs342.project3.http.ClientThread;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.logging.Logger;

/**
 * Singleton scene manager.
 *
 * Construction order is critical:
 *   1. instance field is assigned AFTER the constructor body completes.
 *   2. Both SceneManager and ClientThread are injected into controllers via
 *      setters AFTER FXML loading, so initialize() never touches either singleton.
 */
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
        preloadScenes();
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public static SceneManager getInstance() {
        if (instance == null)
            throw new IllegalStateException("SceneManager not yet initialised");
        return instance;
    }

    public String       getCurrentUsername() { return currentUsername; }
    public String       getCurrentGameId()   { return currentGameId;   }
    public Stage        getStage()           { return stage;           }

    // ── Methods ───────────────────────────────────────────────────────────────

    /**
     * Called exactly once from ClientApplication.start().
     * Returns only after full construction — instance is safe to use after this.
     */
    public static SceneManager create(Stage stage, ClientThread client) throws Exception {
        SceneManager sm = new SceneManager(stage, client);
        instance = sm;
        return sm;
    }

    public void showLogin() {
        LoginController lc = loginLoader.getController();
        lc.reset();
        stage.setTitle("Checkers Online \u2013 Login");
        stage.setScene(loginScene);
        stage.show();
    }

    public void showLobby(String username) {
        this.currentUsername = username;
        LobbyController lob = lobbyLoader.getController();
        lob.onEnter();
        stage.setTitle("Checkers Online \u2013 Lobby  [" + username + "]");
        stage.setScene(lobbyScene);
    }

    public void showGame(String gameId) {
        this.currentGameId = gameId;
        GameController gc = gameLoader.getController();
        gc.onEnter(gameId, currentUsername);
        stage.setTitle("Checkers Online \u2013 Game");
        stage.setScene(gameScene);
    }

    private void preloadScenes() throws Exception {
        loginLoader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
        Parent loginRoot = loginLoader.load();
        loginScene = new Scene(loginRoot, 480, 560);
        loginScene.getStylesheets().add(css("dark-theme"));

        lobbyLoader = new FXMLLoader(getClass().getResource("/fxml/lobby.fxml"));
        Parent lobbyRoot = lobbyLoader.load();
        lobbyScene = new Scene(lobbyRoot, 1100, 700);
        lobbyScene.getStylesheets().add(css("dark-theme"));

        gameLoader = new FXMLLoader(getClass().getResource("/fxml/game.fxml"));
        Parent gameRoot = gameLoader.load();
        gameScene = new Scene(gameRoot, 1200, 760);
        gameScene.getStylesheets().add(css("dark-theme"));

        // Inject both dependencies into each controller after initialize() has run
        LoginController lc  = loginLoader.getController();
        LobbyController lob = lobbyLoader.getController();
        GameController  gc  = gameLoader.getController();

        lc.setSceneManager(this);  lc.setClientThread(client);
        lob.setSceneManager(this); lob.setClientThread(client);
        gc.setSceneManager(this);  gc.setClientThread(client);
    }

    private String css(String name) {
        return getClass().getResource("/css/" + name + ".css").toExternalForm();
    }
}

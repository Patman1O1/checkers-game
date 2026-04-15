package edu.uic.cs342.project3;

import edu.uic.cs342.project3.http.ClientThread;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Entry point for the Checkers Client application.
 * Starts ClientThread then passes it to SceneManager so it can be injected
 * into each controller directly.
 */
public class ClientApplication extends Application {

    // ── Methods ───────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setResizable(false);

        ClientThread client = ClientThread.getInstance();
        client.start();

        SceneManager sceneManager = SceneManager.create(primaryStage, client);
        sceneManager.showLogin();
    }

    @Override
    public void stop() {
        ClientThread.getInstance().close();
    }
}

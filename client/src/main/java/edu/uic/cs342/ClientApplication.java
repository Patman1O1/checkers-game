package edu.uic.cs342;

import edu.uic.cs342.http.ClientThread;
import javafx.application.Application;
import javafx.stage.Stage;

public class ClientApplication extends Application {

    // ── Methods ───────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        ClientApplication.launch(args);
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

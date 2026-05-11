package edu.uic.cs342.project3;

import edu.uic.cs342.project3.http.ClientThread;

import javafx.application.Application;
import javafx.stage.Stage;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientApplication extends Application {
    // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────────
    private static final Logger LOGGER = Logger.getLogger(ClientApplication.class.getName());

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public static void main(String[] args) { Application.launch(args); }

    @Override
    public void start(Stage primaryStage) {
        try {
            primaryStage.setResizable(false);

            ClientThread client = ClientThread.getInstance();
            client.start();

            SceneManager sceneManager = SceneManager.create(primaryStage, client);
            sceneManager.showLogin();
        } catch (Exception exception) {
            ClientApplication.LOGGER.log(Level.SEVERE, exception.getMessage(), exception);
        }
    }

    @Override
    public void stop() { ClientThread.getInstance().close(); }
}

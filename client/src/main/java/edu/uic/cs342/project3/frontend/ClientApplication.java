package edu.uic.cs342.project3.frontend;

import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.stage.Stage;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientApplication extends Application {
    // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────────
    private static final Logger LOGGER = Logger.getLogger(ClientApplication.class.getName());

    private SceneManager sceneManager;

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public static void main(String[] args) { Application.launch(args); }

    @Override
    public void start(Stage primaryStage) {
        try {
            Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

            this.sceneManager = SceneManager.getInstance();
            this.sceneManager.setPrimaryStage(primaryStage);
            this.sceneManager.showLoginScene();

            primaryStage.setTitle("Checkers");
            primaryStage.show();
        } catch (Exception exception) {
            String exceptionMessage = exception.getMessage();
            System.err.printf("%s thrown with message \"%s\"\n", Exception.class.getName(), exceptionMessage);
            ClientApplication.LOGGER.log(Level.SEVERE, exceptionMessage, exception);
        }
    }
}

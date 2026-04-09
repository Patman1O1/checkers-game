package edu.uic.cs342.project3;

import javafx.application.Application;
import javafx.stage.Stage;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerApplication extends Application {
    // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────────
    private static final Logger LOGGER = Logger.getLogger(ServerApplication.class.getName());


    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public static void main(String[] args) { Application.launch(args); }

    @Override
    public void start(Stage primaryStage) {
        try {

            primaryStage.show();
        } catch (Exception exception) {
            ServerApplication.LOGGER.log(Level.SEVERE, exception.getMessage(), exception);
        }
    }
}

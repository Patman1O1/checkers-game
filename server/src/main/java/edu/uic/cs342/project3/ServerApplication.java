package edu.uic.cs342.project3;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerApplication extends Application {
    // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────────
    private static final Logger LOGGER = Logger.getLogger(ServerApplication.class.getName());

    private static final URL FXML = ServerApplication.class.getResource("/fxml/server.fxml");

    private static final URL CSS = ServerApplication.class.getResource("/css/index.css");

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public static void main(String[] args) { Application.launch(args); }

    @Override
    public void start(Stage primaryStage) {
        try {
            // Load FXML
            FXMLLoader fxmlLoader = new FXMLLoader(ServerApplication.FXML);
            Parent root = fxmlLoader.load();
            Scene scene = new Scene(root);

            // Apply CSS
            scene.getStylesheets().add(Objects.requireNonNull(ServerApplication.CSS).toExternalForm());

            // Set the application title
            primaryStage.setTitle("Checkers");

            // Display the application
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception exception) {
            ServerApplication.LOGGER.log(Level.SEVERE, exception.getMessage(), exception);
        }
    }
}

package edu.uic.cs342.project3.frontend;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
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
    public void start(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(ServerApplication.FXML);
            Scene scene = new Scene(loader.load(), 960, 640);
            scene.getStylesheets().add(Objects.requireNonNull(ServerApplication.CSS).toExternalForm());

            stage.setTitle("Checkers Server");
            stage.setScene(scene);
            stage.show();
        } catch (Exception exception) {
            ServerApplication.LOGGER.log(Level.SEVERE, exception.getMessage(), exception);
        }
    }
}

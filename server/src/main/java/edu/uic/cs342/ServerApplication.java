package edu.uic.cs342;

import edu.uic.cs342.http.ServerThread;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerApplication extends Application {
    // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────────
    private static final Logger LOGGER = Logger.getLogger(ServerApplication.class.getName());

    private static final String CSS = Objects.requireNonNull(ServerApplication.class.getResource("/css/index.css")).toExternalForm();

    private ServerThread httpServer;

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public static void main(String[] args) { Application.launch(args); }

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(ServerController.SCENE_FXML);
            ServerController controller = loader.getController();

            this.httpServer = new ServerThread(controller::appendLog);
            this.httpServer.start();

            controller.appendLog(String.format("Checkers Server started on port %d", ServerThread.DEFAULT_PORT));

            Scene scene = new Scene(loader.load(), ServerController.SCENE_WIDTH, ServerController.SCENE_HEIGHT);
            scene.getStylesheets().add(ServerApplication.CSS);

            primaryStage.setTitle("Checkers Server");
            primaryStage.setScene(scene);
            primaryStage.setOnCloseRequest(event -> this.httpServer.stopServer());
            primaryStage.show();
        } catch (Exception exception) {
            ServerApplication.LOGGER.log(Level.SEVERE, exception.getMessage(), exception);
        }
    }

    @Override
    public void stop() {
        if (this.httpServer != null) {
            this.httpServer.stopServer();
        }
    }
}

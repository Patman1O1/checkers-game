package edu.uic.cs342.project3;

import edu.uic.cs342.project3.http.ServerThread;

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

    private static final URL FXML_URL = ServerApplication.class.getResource("/fxml/server.fxml");

    private static final URL CSS_URL = Objects.requireNonNull(ServerApplication.class.getResource("/css/server.css"));

    private static final int DEFAULT_SCENE_WIDTH = 820;

    private static final int DEFAULT_SCENE_HEIGHT = 560;

    private ServerThread serverThread;

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public static void main(String[] args) { Application.launch(args); }

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(ServerApplication.FXML_URL);
            Parent root = loader.load();
            ServerController controller = loader.getController();

            this.serverThread = new ServerThread(controller::appendLog);
            this.serverThread.start();

            controller.appendLog("Checkers Server started on port " + ServerThread.DEFAULT_PORT);

            Scene scene = new Scene(root, ServerApplication.DEFAULT_SCENE_WIDTH, ServerApplication.DEFAULT_SCENE_HEIGHT);
            scene.getStylesheets().add(ServerApplication.CSS_URL.toExternalForm());

            primaryStage.setTitle("Checkers Server");
            primaryStage.setScene(scene);
            primaryStage.setOnCloseRequest(event -> this.serverThread.interrupt());
            primaryStage.show();
        } catch (Exception exception) {
            ServerApplication.LOGGER.log(Level.SEVERE, exception.getMessage(), exception);
        }
    }

    @Override
    public void stop() {
        if (this.serverThread != null) {
            this.serverThread.interrupt();
        }
    }
}

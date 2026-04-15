package edu.uic.cs342.project3;

import edu.uic.cs342.project3.http.ServerThread;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.logging.Logger;

/**
 * Entry point for the Checkers Server application.
 * Starts the HTTP server on a background thread, then shows the log GUI.
 */
public class ServerApplication extends Application {

    // ── Fields ────────────────────────────────────────────────────────────────

    private static final Logger LOG = Logger.getLogger(ServerApplication.class.getName());

    private ServerThread httpServer;

    // ── Methods ───────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/server.fxml"));
        Parent root = loader.load();
        ServerController controller = loader.getController();

        httpServer = new ServerThread(controller::appendLog);
        httpServer.start();

        controller.appendLog("Checkers Server started on port " + ServerThread.PORT);

        Scene scene = new Scene(root, 820, 560);
        scene.getStylesheets().add(getClass().getResource("/css/server.css").toExternalForm());

        primaryStage.setTitle("Checkers Server");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> httpServer.stopServer());
        primaryStage.show();
    }

    @Override
    public void stop() {
        if (httpServer != null) httpServer.stopServer();
    }
}

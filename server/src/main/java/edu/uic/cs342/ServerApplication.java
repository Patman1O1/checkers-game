package edu.uic.cs342;

import edu.uic.cs342.http.ServerThread;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.logging.Logger;

public class ServerApplication extends Application {

    // ── Fields ────────────────────────────────────────────────────────────────

    private static final Logger LOG = Logger.getLogger(ServerApplication.class.getName());

    private ServerThread httpServer;

    // ── Methods ───────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        ServerApplication.launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(this.getClass().getResource("/fxml/server.fxml"));
        Parent root = loader.load();
        ServerController controller = loader.getController();

        this.httpServer = new ServerThread(controller::appendLog);
        this.httpServer.start();

        controller.appendLog("Checkers Server started on port " + ServerThread.PORT);

        Scene scene = new Scene(root, 820, 560);
        scene.getStylesheets().add(this.getClass().getResource("/css/server.css").toExternalForm());

        primaryStage.setTitle("Checkers Server");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> this.httpServer.stopServer());
        primaryStage.show();
    }

    @Override
    public void stop() {
        if (this.httpServer != null) this.httpServer.stopServer();
    }
}

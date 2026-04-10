package edu.uic.cs342.project3;

import javafx.application.Application;
import javafx.stage.Stage;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientApplication extends Application {
    private static final Logger LOGGER = Logger.getLogger(ClientApplication.class.getName());

    public static void main(String[] args) { Application.launch(args); }

    @Override
    public void start(Stage primaryStage) {
        try {

        } catch (Exception exception) {
            ClientApplication.LOGGER.log(Level.SEVERE, exception.getMessage(), exception);
        }
    }
}

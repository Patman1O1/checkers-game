package edu.cs342.project2;

import javafx.application.Application;
import javafx.stage.Stage;

public class WeatherApplication extends Application {
    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage primaryStage) {
        try {

            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

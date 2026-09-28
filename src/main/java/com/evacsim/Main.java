package com.evacsim;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * Entry point: boots JavaFX, loads main.fxml, shows the window.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(getClass().getResource("/com/evacsim/main.fxml")));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1080, 900);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/com/evacsim/styles.css"))
                        .toExternalForm());

        primaryStage.setTitle("Crowd Evacuation Simulator");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(960);
        primaryStage.setMinHeight(720);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

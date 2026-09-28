package com.evacsim;

import com.evacsim.concurrent.SimulationExecutors;
import com.evacsim.controller.LoginController;
import com.evacsim.controller.SimulationController;
import com.evacsim.db.Database;
import com.evacsim.db.Operator;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * Entry point: SQLite bootstrap, login, then compact simulation window.
 */
public class Main extends Application {

    private Stage stage;

    @Override
    public void start(Stage primaryStage) throws IOException {
        this.stage = primaryStage;
        Database.initialize();
        showLogin();
    }

    private void showLogin() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(getClass().getResource("/com/evacsim/login.fxml")));
        Parent root = loader.load();
        LoginController controller = loader.getController();
        controller.setOnSuccess(this::showSimulation);

        Scene scene = new Scene(root, 420, 340);
        scene.getStylesheets().add(css());
        stage.setTitle("Evacuation Control — Login");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setMaximized(false);
        stage.centerOnScreen();
        stage.show();
    }

    private void showSimulation(Operator operator) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(getClass().getResource("/com/evacsim/main.fxml")));
            Parent root = loader.load();
            SimulationController controller = loader.getController();
            controller.setOperator(operator);
            controller.setOnLogout(() -> {
                try {
                    showLogin();
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });

            Scene scene = new Scene(root, 900, 620);
            scene.getStylesheets().add(css());
            controller.bindResponsiveLayout(scene);

            stage.setTitle("Crowd Evacuation Simulator");
            stage.setScene(scene);
            stage.setResizable(true);
            stage.setMaximized(false);
            stage.setMinWidth(760);
            stage.setMinHeight(520);
            stage.setWidth(900);
            stage.setHeight(620);
            stage.centerOnScreen();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load simulation UI", e);
        }
    }

    private static String css() {
        return Objects.requireNonNull(Main.class.getResource("/com/evacsim/styles.css")).toExternalForm();
    }

    @Override
    public void stop() {
        SimulationExecutors.shutdown();
        Database.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

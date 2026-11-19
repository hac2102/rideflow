package com.rideflow.desktop;

import com.rideflow.desktop.client.BackendApiClient;
import com.rideflow.desktop.controller.DashboardController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class RideFlowDesktopApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        BackendApiClient client = new BackendApiClient("http://localhost:8080");
        DashboardController controller = new DashboardController(client);

        Scene scene = new Scene(controller.getRootPane(), 1280, 800);
        primaryStage.setTitle("RideFlow — Operations Dashboard & City Simulator");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);

        primaryStage.setOnCloseRequest(e -> System.exit(0));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

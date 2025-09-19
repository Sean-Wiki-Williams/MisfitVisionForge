package com.visonforge.visionforge;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class VisionForgeApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(VisionForgeApplication.class.getResource("VisionForge.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        stage.setTitle("VisionForge");
        stage.setScene(scene);
        stage.setMaximized(true);
        URL icon = getClass().getResource("icon.png");
        stage.getIcons().add(new Image(icon.toString()));
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
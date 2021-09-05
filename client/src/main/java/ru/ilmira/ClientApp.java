package ru.ilmira;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.InputStream;
import java.util.Objects;

public class ClientApp extends Application {
    public static Stage primaryStage;

    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        ClientApp.primaryStage = primaryStage;

        InputStream iconStream = getClass().getResourceAsStream("/icons/icon.png");
        Image image = new Image(Objects.requireNonNull(iconStream));
        primaryStage.getIcons().add(image);

        primaryStage.setTitle("ChatTest");
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/chatWindow.fxml")));
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }
}

package ru.ilmira.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import ru.ilmira.ChatConnection;
import ru.ilmira.UserProperties;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

@Slf4j
public class AuthWindowController {
    @FXML
    private TextField loginTF;
    @FXML
    private PasswordField passwordTF;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private void connect() {
        try {
            socket = ChatConnection.getSocket();
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            log.error("Ошибка подключения!", e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка подключения!");
            alert.setHeaderText("Сервер не запущен!");
            alert.showAndWait();
            e.printStackTrace();
            System.exit(0);
        }
    }

    @FXML
    private void initialize() {

        connect();

        new Thread(() -> {
            try {
                while (true) {
                    String serverMsg = in.readUTF();
                    if (serverMsg.startsWith("/authok")) {
                        UserProperties.login = loginTF.getText();
                        UserProperties.nickName = serverMsg.split(" ")[1];
                        Platform.runLater(() -> {
                            Stage stage = (Stage) loginTF.getScene().getWindow();
                            stage.close();
                        });
                        break;
                    } else {
                        Platform.runLater(() -> {
                            log.warn("Авторизация не удалась!");
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Ошибка авторизации!");
                            alert.setHeaderText("Авторизация не удалась!");
                            alert.setContentText(serverMsg);
                            alert.showAndWait();
                        });
                    }
                }
            } catch (Exception e) {
                log.error("", e);
                //e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void auth() throws IOException {
        String authString = "/auth " + loginTF.getText() + " " + passwordTF.getText();
        out.writeUTF(authString);
    }

    @FXML
    private void cancel() {
        try {
            socket.close();
            out.close();
            in.close();
        } catch (IOException e) {
            log.error("", e);
            //e.printStackTrace();
        }
        System.exit(0);
    }

    @FXML
    public void noAuth(ActionEvent actionEvent) throws IOException {
        String authString = "/noauth";
        out.writeUTF(authString);
    }
}

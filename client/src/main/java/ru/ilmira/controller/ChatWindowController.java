package ru.ilmira.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import ru.ilmira.ChatConnection;
import ru.ilmira.ClientApp;
import ru.ilmira.UserProperties;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class ChatWindowController {
    @FXML
    private Label nickNameLBL;
    @FXML
    private TextArea messageTA;

    private final ObservableList<String> clientList = FXCollections.observableArrayList();
    @FXML
    private ListView<String> clientListLV;

    @FXML
    private TextField inputTF;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    @FXML
    private void sendMessage(ActionEvent event) {
        inputTF.requestFocus();
        if (inputTF.getText().isEmpty()) return;
        sendMessage(inputTF.getText());
        messageTA.appendText(inputTF.getText() + "\n");
        inputTF.clear();
    }

    private void sendMessage(String s) {
        try {
            out.writeUTF(s);
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Ошибка отправки сообщения");
            alert.setHeaderText("Ошибка отправки сообщения");
            alert.setContentText("При отправке сообщения возникла ошибка: " + e.getMessage());
            alert.show();
        }
    }

    @FXML
    private void initialize() {
        try {
            openLoginWindow();
            nickNameLBL.setText(UserProperties.nickName);
            openConnection();
            addCloseListener();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void addCloseListener() {
        EventHandler<WindowEvent> onCloseRequest = ClientApp.primaryStage.getOnCloseRequest();
        ClientApp.primaryStage.setOnCloseRequest(event -> {
            closeConnection();
            if (onCloseRequest != null) {
                onCloseRequest.handle(event);
            }
        });
    }

    private void openLoginWindow() throws IOException {
        Parent parent = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/authWindow.fxml")));
        Stage loginStage = new Stage();
        loginStage.setResizable(false);
        loginStage.initModality(Modality.APPLICATION_MODAL);
        loginStage.setScene(new Scene(parent));
        loginStage.setTitle("Авторизация");
        loginStage.setOnCloseRequest(event -> System.exit(0));
        loginStage.showAndWait();
    }

    private void openConnection() throws IOException {
        socket = ChatConnection.getSocket();
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());

        new Thread(() -> {
            try {
                while (socket.isConnected()) {
                    String msg = in.readUTF();
                    if (msg.equalsIgnoreCase("/end")) {
                        break;
                    } else if (msg.startsWith("/clientsonline")) {
                        // получаем список клиентов в сети после авторизации
                        String[] clientsOnline = msg.split(" ");
                        clientList.addAll(Arrays.asList(clientsOnline).subList(1, clientsOnline.length));
                        clientListLV.setItems(clientList);
                    } else if (msg.startsWith("/cliententry")) {
                        // добавляем в список нового клиента, подключившегося к чату
                        Platform.runLater(() -> {
                            String nickName = msg.split(" ")[1];
                            clientList.add(nickName);
                            clientListLV.setItems(clientList);
                            messageTA.appendText(nickName + " зашел в чат \n");
                        });
                    } else if (msg.startsWith("/clientexit")) {
                        // удаляем клиента, отключившегося от чата
                        Platform.runLater(() -> {
                            String nickName = msg.split(" ")[1];
                            clientList.remove(nickName);
                            clientListLV.setItems(clientList);
                            messageTA.appendText(nickName + " вышел из чата \n");
                        });
                    } else if (msg.startsWith("/newnicknameok")) {
                        Platform.runLater(() -> {
                            String oldNickName = msg.split(" ")[1];
                            String newNickName = msg.split(" ")[2];
                            if (oldNickName.equals(UserProperties.nickName)) {
                                UserProperties.nickName = newNickName;
                                nickNameLBL.setText(UserProperties.nickName);
                            } else {
                                clientList.remove(oldNickName);
                                clientList.add(newNickName);
                                clientListLV.setItems(clientList);
                            }
                        });
                    } else {
                        messageTA.appendText(msg + "\n");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    messageTA.appendText("Соединение с сервером разорвано...");
                    clientList.clear();
                    in.close();
                    out.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void closeConnection() {
        try {
            out.writeUTF("/end");
            socket.close();
            out.close();
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void selectClient(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            final String msg = inputTF.getText();
            String nickname = clientListLV.getSelectionModel().getSelectedItem();
            inputTF.setText("/w " + nickname + " " + msg);
            inputTF.requestFocus();
            inputTF.selectEnd();
        }
    }

    public void changeNickName(ActionEvent actionEvent) {
        TextInputDialog inputDialog = new TextInputDialog("");
        inputDialog.setTitle("Новый никнейм");
        inputDialog.setHeaderText("Введите новый никнейм");
        inputDialog.showAndWait()
                .map(nick -> nick.trim())
                .filter(nick -> !"".equals(nick))
                .map(nick -> {
                    sendMessage("/newnickname " +nick);
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setContentText(nick);
                    return alert;
                })
                .ifPresent(alert -> alert.showAndWait());
    }
}

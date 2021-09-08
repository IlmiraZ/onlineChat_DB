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

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

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

    private String logFileName;
    private FileWriter file;

    @FXML
    private void sendMessage(ActionEvent event) {
        inputTF.requestFocus();
        if (inputTF.getText().isEmpty()) return;
        sendMessage(inputTF.getText());
        messageTA.appendText(inputTF.getText() + "\n");
        writeFile(UserProperties.nickName + ": " + inputTF.getText());
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
            logFileName = "history_" + UserProperties.login + ".txt";
            openLogFile(logFileName);
            loadHistory(logFileName, 100);
            addCloseListener();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openLogFile(String fileName) {
        try {
            file = new FileWriter(fileName,true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeFile(String textMsg) {
        try {
            file.write(textMsg + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void addCloseListener() {
        EventHandler<WindowEvent> onCloseRequest = ClientApp.primaryStage.getOnCloseRequest();
        ClientApp.primaryStage.setOnCloseRequest(event -> {
            closeConnection();
            closeFile();
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
                            String s = nickName + " зашел в чат \n";
                            messageTA.appendText(s);
                            writeFile(s);
                        });
                    } else if (msg.startsWith("/clientexit")) {
                        // удаляем клиента, отключившегося от чата
                        Platform.runLater(() -> {
                            String nickName = msg.split(" ")[1];
                            clientList.remove(nickName);
                            clientListLV.setItems(clientList);
                            String s = nickName + " вышел из чата \n";
                            messageTA.appendText(s);
                            writeFile(s);
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
                        writeFile(msg + "\n");
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

    private void closeFile() {
        try {
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    private void loadHistory(String fileName, int lineCount) {
        if (lineCount == 0) return;
        File file = new File(fileName);
        StringBuilder builder = new StringBuilder();
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(fileName, "r")) {
            long pos = file.length();
            if (pos == 0) return;
            randomAccessFile.seek(pos);

            int temp = lineCount;
            for (long i = pos - 1; i >= 0; i--) {
                randomAccessFile.seek(i);
                char ch = (char) randomAccessFile.read();
                if (ch == '\n') temp--;
                if (temp == 0) break;
                builder.append(ch);
            }
            builder.reverse();
            messageTA.appendText(new String(builder.toString().getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

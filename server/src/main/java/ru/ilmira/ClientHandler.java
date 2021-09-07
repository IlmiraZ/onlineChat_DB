package ru.ilmira;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {

    private final MyServer myServer;
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;

    private String login;
    private String nickName;
    private static final int SESSION_LIMIT = 12000;
    private Long anonymEntryTime;
    private static int anonymNum = 0;

    public String getNickName() {
        return nickName;
    }

    private static synchronized int getAnonymNum() {
        return ++anonymNum;
    }

    public ClientHandler(MyServer myServer, Socket socket) {
        try {
            this.myServer = myServer;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            this.nickName = "";
            new Thread(() -> {
                try {
                    authentication();
                    readMessage();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    closeConnection();
                }
            }).start();
        } catch (IOException e) {
            throw new RuntimeException("Проблемы при создании обработчика клиента!");
        }
    }

    public void authentication() throws IOException {
        while (true) {
            String msg = in.readUTF();
            if (msg.startsWith("/auth")) {
                doClientLogin(msg);
                break;
            } else if (msg.startsWith("/noauth")) {
                doAnonymousLogin();
                break;
            }
        }
    }

    public void doClientLogin(String msg) {
        String[] parts = msg.split("\\s");
        nickName = myServer.getAuthService().getNickByLoginPass(parts[1], parts[2]);
        if (nickName != null) {
            if (!myServer.isNickBusy(nickName)) {
                login = parts[1];
                sendMsg("/authok " + nickName);
                sendMsg("/clientsonline " + myServer.getClientsOnline(this));
                myServer.broadcastMsg("/cliententry " + nickName);
                myServer.subscribe(this);
            } else {
                sendMsg("Учетная запись уже используется!!!");
            }
        } else {
            sendMsg("Неверные логин/пароль!");
        }
    }

    private void doAnonymousLogin() {
        nickName = "Anonymous" + ClientHandler.getAnonymNum();
        sendMsg("/authok " + nickName);
        sendMsg("Сессия для неавторизованных пользователей ограничена " + SESSION_LIMIT / 1000 + " сек.");
        sendMsg("clientsonline " + myServer.getClientsOnline(this));
        myServer.broadcastMsg("/cliententry " + nickName);
        myServer.subscribe(this);
        anonymEntryTime = System.currentTimeMillis();

        new Thread(() -> {
            try {
                while (true) {
                    if (System.currentTimeMillis() - anonymEntryTime >= SESSION_LIMIT) {
                        sendMsg("Для продолжения общения в чате необходимо авторизоваться!");
                        break;
                    }
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                try {
                    in.close();
                    out.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void readMessage() throws IOException {
        while (true) {
            String clientMsg = in.readUTF();
            System.out.println("от " + nickName + ": " + clientMsg);

            if (clientMsg.equalsIgnoreCase("/end")) {
                break;
            } else {
                if (clientMsg.startsWith("/w ")) {
                    String[] parts = clientMsg.split(" ");
                    String nickNameTo = parts[1];
                    String msgText = clientMsg.substring(3 + nickNameTo.length() + 1);
                    myServer.sendPersonalMessage(this, nickNameTo, msgText);
                } else if (clientMsg.startsWith("/newnickname ")){
                    String[] parts = clientMsg.split(" ");
                    String newNickName = parts[1];
                    if (myServer.getAuthService().changeNickName(this.login, newNickName)) {
                        myServer.broadcastMsg("/newnicknameok " + this.nickName + " " + newNickName);
                        myServer.broadcastMsg(this.nickName + " сменил имя на " + newNickName);
                        this.nickName = newNickName;
                    }
                } else {
                    myServer.broadcastMsg(this, clientMsg);
                }
            }
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeConnection() {
        myServer.unsubscribe(this);
        myServer.broadcastMsg("/clientexit " + nickName);
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

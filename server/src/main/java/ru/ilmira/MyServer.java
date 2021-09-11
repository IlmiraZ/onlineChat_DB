package ru.ilmira;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyServer {
    private List<ClientHandler> clients;
    private AuthService authService;

    private ExecutorService executorService;

    public AuthService getAuthService() {
        return authService;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public MyServer() {
        int PORT = 3344;
        try (ServerSocket server = new ServerSocket(PORT)) {
            authService = new BaseAuthService();
            executorService = Executors.newCachedThreadPool();
            authService.connect();

            clients = new ArrayList<>();
            while (true) {
                System.out.println("Сервер ожидает подключения...");
                Socket socket = server.accept();
                System.out.println("Клиент подключился...");
                new ClientHandler(this, socket);
            }
        } catch (IOException | SQLException e) {
            System.out.println("Ошибка в работе сервера!");
        } finally {
            if (authService != null) {
                executorService.shutdown();
                authService.disconnect();
            }
        }
    }

    public synchronized boolean isNickBusy(String nick) {
        for (ClientHandler o : clients) {
            if (o.getNickName().equals(nick)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void sendPersonalMessage(ClientHandler fromClient, String toNickName, String msg) {
        for (ClientHandler client : clients) {
            if (client.getNickName().equals(toNickName)) {
                if (!fromClient.getNickName().equals(toNickName)) {
                    client.sendMsg(fromClient.getNickName() + ": " + msg);
                }
                return;
            }
        }
        fromClient.sendMsg("Участник с ником \"" + toNickName + "\" не найден!");
    }

    public synchronized void broadcastMsg(String msg) {
        for (ClientHandler o : clients) {
            o.sendMsg(msg);
        }
    }

    public synchronized void broadcastMsg(ClientHandler fromClient, String msg) {
        for (ClientHandler client : clients) {
            if (client != fromClient) {
                client.sendMsg(fromClient.getNickName() + ": " + msg);
            }
        }
    }

    public synchronized void unsubscribe(ClientHandler o) {
        clients.remove(o);
    }

    public synchronized void subscribe(ClientHandler o) {
        clients.add(o);
    }

    public String getClientsOnline(ClientHandler fromClient) {
        StringBuilder result = new StringBuilder();
        for (ClientHandler client : clients) {
            if (client != fromClient) {
                result.append(client.getNickName()).append(" ");
            }
        }
        return result.toString().trim();
    }
}

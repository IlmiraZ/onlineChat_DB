package ru.ilmira;

import java.io.IOException;
import java.net.Socket;

public class ChatConnection {
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 3344;
    private static Socket socket;

    public static Socket getSocket() throws IOException {
        if (socket == null) {
            socket = new Socket(SERVER_IP, SERVER_PORT);
        }
        return socket;
    }
}

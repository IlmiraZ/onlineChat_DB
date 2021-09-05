package ru.ilmira;

import java.sql.SQLException;

public interface AuthService {
    void connect() throws SQLException;
    void disconnect();
    String getNickByLoginPass(String login, String pass);
    boolean changeNickName(String login, String newNickName);
}

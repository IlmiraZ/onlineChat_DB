package ru.ilmira;

import lombok.extern.slf4j.Slf4j;

import java.sql.*;

@Slf4j
public class BaseAuthService implements AuthService {

    private Connection connection;
    private Statement statement;
    private PreparedStatement preparedStatement;

    @Override
    public void connect() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:chatOnline.db");
        statement = connection.createStatement();
        log.info("Сервис аутентификации запущен!");
        // System.out.println("Сервис аутентификации запущен!");
    }

    @Override
    public void disconnect() {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                log.error("", e);
                //e.printStackTrace();
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                log.error("", e);
                //e.printStackTrace();
            }
        }
        log.info("Сервис аутентификации остановлен!");
        //System.out.println("Сервис аутентификации остановлен!");
    }

    @Override
    public String getNickByLoginPass(String login, String pass) {
        try {
            if (connection == null || connection.isClosed()) return null;

            preparedStatement = connection.prepareStatement("select nickname from users where login = ? and password = ?");
            preparedStatement.setString(1, login);
            preparedStatement.setString(2, pass);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                return rs.getString("nickname");
            } else {
                log.info("Неправильно введен логин и/или пароль");
                //System.out.println("Неправильно введен логин и/или пароль");
                return null;
            }
        } catch (SQLException e) {
            log.error("", e);
            //e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean changeNickName(String login, String newNickName) {
        try {
            if (connection == null || connection.isClosed()) return false;

            preparedStatement = connection.prepareStatement("update users set nickname = ? where login = ?");
            preparedStatement.setString(1, newNickName);
            preparedStatement.setString(2, login);
            int resUpdated = preparedStatement.executeUpdate();
            if (resUpdated == 0) {
                log.info(" Не удалось изменить nickName у пользователя с логином " + login);
                //System.out.println(" Не удалось изменить nickName у пользователя с логином " + login);
            }
            return resUpdated > 0;
        } catch (SQLException e) {
            log.error("", e);
            //e.printStackTrace();
        }
        return false;
    }
}

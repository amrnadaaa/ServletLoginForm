package com.ecommerce.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {
    private static final String DB_URL = AppConfig.getProperty("db.url");
    private static final String DB_USER = AppConfig.getProperty("db.user");
    private static final String DB_PASSWORD = AppConfig.getProperty("db.password");
    private static final String DB_DRIVER = AppConfig.getProperty("db.driver");

    static {
        try {
            Class.forName(DB_DRIVER);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load database driver", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
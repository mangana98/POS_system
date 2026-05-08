package pos.db;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {
    private static final string HOST = "STEPHEN-PC"
    private static final String URL = "jdbc:mysql://localhost:3306/pos_system";
    private static final String USER = "root";
    private static final String PASS = "";

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}

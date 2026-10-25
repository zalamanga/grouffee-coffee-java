package id.demo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Db {
  private Db() {}

  public static Connection open() throws SQLException {
    DriverLifecycle.loadDriver();
    String password = System.getenv("DB_PASSWORD");
    if (password == null || password.isBlank()) throw new SQLException("DB_PASSWORD is required");
    String name = env("DB_NAME", "grouffee");
    String host = env("DB_HOST", "127.0.0.1");
    String port = env("DB_PORT", "3306");
    if (!name.matches("[A-Za-z0-9_]+")
        || !host.matches("[A-Za-z0-9.:-]+")
        || !port.matches("[0-9]{1,5}")) {
      throw new SQLException("Invalid database configuration");
    }
    return DriverManager.getConnection(
        "jdbc:mysql://"
            + host
            + ":"
            + port
            + "/"
            + name
            + "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC&connectTimeout=5000&socketTimeout=10000",
        env("DB_USER", "demo_app"),
        password);
  }

  public static String env(String name, String fallback) {
    String value = System.getenv(name);
    return value == null || value.isBlank() ? fallback : value;
  }
}

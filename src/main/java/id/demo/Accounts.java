package id.demo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class Accounts {
  private Accounts() {}

  static Account read(ResultSet rs) throws SQLException {
    return new Account(
        rs.getLong("id"),
        rs.getString("username"),
        rs.getString("name"),
        rs.getString("email"),
        rs.getString("role"),
        rs.getString("password_hash"));
  }

  public static Account find(long id) throws SQLException {
    try (Connection c = Db.open();
        PreparedStatement p = c.prepareStatement("SELECT * FROM accounts WHERE id=?")) {
      p.setLong(1, id);
      try (ResultSet rs = p.executeQuery()) {
        return rs.next() ? read(rs) : null;
      }
    }
  }

  public static Account login(String username, String password) throws SQLException {
    try (Connection c = Db.open();
        PreparedStatement p = c.prepareStatement("SELECT * FROM accounts WHERE username=?")) {
      p.setString(1, username);
      try (ResultSet rs = p.executeQuery()) {
        Account account = rs.next() ? read(rs) : null;
        return account != null && Passwords.verify(password, account.getHash()) ? account : null;
      }
    }
  }

  public static void register(String username, String name, String email, String password)
      throws SQLException {
    String hash = Passwords.hash(password);
    try (Connection c = Db.open();
        PreparedStatement p =
            c.prepareStatement(
                "INSERT INTO accounts(username,name,email,password_hash,role) VALUES(?,?,?,?,'member')")) {
      p.setString(1, username);
      p.setString(2, name);
      p.setString(3, email);
      p.setString(4, hash);
      p.executeUpdate();
    }
  }

  public static void changePassword(Account account, String oldPassword, String newPassword)
      throws SQLException {
    if (!Passwords.verify(oldPassword, account.getHash()))
      throw new IllegalArgumentException("Kata sandi saat ini tidak sesuai.");
    String hash = Passwords.hash(newPassword);
    try (Connection c = Db.open();
        PreparedStatement p =
            c.prepareStatement(
                "UPDATE accounts SET password_hash=? WHERE id=? AND password_hash=?")) {
      p.setString(1, hash);
      p.setLong(2, account.getId());
      p.setString(3, account.getHash());
      if (p.executeUpdate() != 1)
        throw new IllegalArgumentException("Akun berubah. Muat ulang dan coba lagi.");
    }
  }

  public static List<Account> all() throws SQLException {
    List<Account> result = new ArrayList<>();
    try (Connection c = Db.open();
        PreparedStatement p = c.prepareStatement("SELECT * FROM accounts ORDER BY id");
        ResultSet rs = p.executeQuery()) {
      while (rs.next()) result.add(read(rs));
    }
    return result;
  }

  public static void bootstrapAdmin() throws SQLException {
    String username = Validation.username(Db.env("ADMIN_USERNAME", "admin_demo"));
    String password = Validation.password(System.getenv("ADMIN_PASSWORD"));
    try (Connection c = Db.open();
        PreparedStatement lock = c.prepareStatement("SELECT GET_LOCK('grouffee_bootstrap',15)")) {
      try (ResultSet rs = lock.executeQuery()) {
        if (!rs.next() || rs.getInt(1) != 1) throw new SQLException("Bootstrap lock unavailable");
      }
      try {
        try (PreparedStatement p =
                c.prepareStatement("SELECT COUNT(*) FROM accounts WHERE role='admin'");
            ResultSet rs = p.executeQuery()) {
          rs.next();
          if (rs.getInt(1) > 0) return;
        }
        try (PreparedStatement p =
            c.prepareStatement(
                "INSERT INTO accounts(username,name,email,password_hash,role) VALUES(?,'Admin demo','admin@example.test',?,'admin')")) {
          p.setString(1, username);
          p.setString(2, Passwords.hash(password));
          p.executeUpdate();
        }
      } finally {
        try (PreparedStatement p =
            c.prepareStatement("SELECT RELEASE_LOCK('grouffee_bootstrap')")) {
          p.execute();
        }
      }
    }
  }
}

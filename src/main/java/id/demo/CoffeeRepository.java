package id.demo;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public final class CoffeeRepository {
  public static final Set<String> IMAGES =
      Set.of(
          "cappuccino.png",
          "americano.png",
          "frappuccino.png",
          "flatwhite.png",
          "heartcoffee.png",
          "croissant.png");

  private CoffeeRepository() {}

  private static Map<String, Object> row(ResultSet rs) throws SQLException {
    Map<String, Object> result = new LinkedHashMap<>();
    ResultSetMetaData meta = rs.getMetaData();
    for (int i = 1; i <= meta.getColumnCount(); i++)
      result.put(meta.getColumnLabel(i), rs.getObject(i));
    return result;
  }

  public static List<Map<String, Object>> menu() throws SQLException {
    try (Connection c = Db.open();
        PreparedStatement p = c.prepareStatement("SELECT * FROM menu ORDER BY category,name");
        ResultSet rs = p.executeQuery()) {
      List<Map<String, Object>> result = new ArrayList<>();
      while (rs.next()) result.add(row(rs));
      return result;
    }
  }

  public static Map<String, Object> item(long id) throws SQLException {
    try (Connection c = Db.open();
        PreparedStatement p = c.prepareStatement("SELECT * FROM menu WHERE id=?")) {
      p.setLong(1, id);
      try (ResultSet rs = p.executeQuery()) {
        return rs.next() ? row(rs) : null;
      }
    }
  }

  public static void saveMenu(long id, String name, String category, BigDecimal price, String image)
      throws SQLException {
    if (category == null
        || image == null
        || !Set.of("Minuman", "Makanan").contains(category)
        || !IMAGES.contains(image))
      throw new IllegalArgumentException("Pilih kategori dan gambar yang tersedia.");
    try (Connection c = Db.open();
        PreparedStatement p =
            c.prepareStatement(
                id == 0
                    ? "INSERT INTO menu(name,category,price,image) VALUES(?,?,?,?)"
                    : "UPDATE menu SET name=?,category=?,price=?,image=? WHERE id=?")) {
      p.setString(1, name);
      p.setString(2, category);
      p.setBigDecimal(3, price);
      p.setString(4, image);
      if (id != 0) p.setLong(5, id);
      if (p.executeUpdate() != 1)
        throw new IllegalArgumentException("Menu tidak ditemukan. Muat ulang daftar menu.");
    }
  }

  public static void deleteMenu(long id) throws SQLException {
    try (Connection c = Db.open();
        PreparedStatement p = c.prepareStatement("DELETE FROM menu WHERE id=?")) {
      p.setLong(1, id);
      if (p.executeUpdate() != 1) throw new IllegalArgumentException("Menu sudah tidak tersedia.");
    }
  }

  public static List<Map<String, Object>> cartRows(Map<Long, Integer> items) throws SQLException {
    List<Map<String, Object>> result = new ArrayList<>();
    for (Map<String, Object> item : menu()) {
      Integer quantity = items.get(((Number) item.get("id")).longValue());
      if (quantity != null) {
        item.put("quantity", quantity);
        item.put(
            "subtotal", ((BigDecimal) item.get("price")).multiply(BigDecimal.valueOf(quantity)));
        result.add(item);
      }
    }
    if (result.size() != items.size())
      throw new IllegalArgumentException(
          "Ada menu yang dihapus. Kosongkan keranjang lalu pilih ulang menu yang tersedia.");
    return result;
  }

  public static BigDecimal total(List<Map<String, Object>> rows) {
    return rows.stream()
        .map(r -> (BigDecimal) r.get("subtotal"))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public static long checkout(long userId, int table, Map<Long, Integer> items, String nonce)
      throws SQLException {
    if (items.isEmpty())
      throw new IllegalArgumentException("Keranjang masih kosong. Tambahkan menu terlebih dahulu.");
    try (Connection c = Db.open()) {
      c.setAutoCommit(false);
      try {
        try (PreparedStatement p =
            c.prepareStatement(
                "SELECT id FROM demo_orders WHERE account_id=? AND checkout_token=?")) {
          p.setLong(1, userId);
          p.setString(2, nonce);
          try (ResultSet rs = p.executeQuery()) {
            if (rs.next()) {
              c.rollback();
              return rs.getLong(1);
            }
          }
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        try (PreparedStatement p = c.prepareStatement("SELECT * FROM menu WHERE id=? FOR SHARE")) {
          for (Map.Entry<Long, Integer> entry : items.entrySet()) {
            p.setLong(1, entry.getKey());
            try (ResultSet rs = p.executeQuery()) {
              if (!rs.next())
                throw new IllegalArgumentException(
                    "Menu berubah. Periksa dan kosongkan keranjang sebelum mencoba lagi.");
              Map<String, Object> row = row(rs);
              row.put("quantity", entry.getValue());
              row.put(
                  "subtotal",
                  rs.getBigDecimal("price").multiply(BigDecimal.valueOf(entry.getValue())));
              rows.add(row);
            }
          }
        }
        long orderId;
        try (PreparedStatement p =
            c.prepareStatement(
                "INSERT INTO demo_orders(account_id,table_number,total,checkout_token) VALUES(?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
          p.setLong(1, userId);
          p.setInt(2, table);
          p.setBigDecimal(3, total(rows));
          p.setString(4, nonce);
          p.executeUpdate();
          try (ResultSet rs = p.getGeneratedKeys()) {
            if (!rs.next()) throw new SQLException("Missing order id");
            orderId = rs.getLong(1);
          }
        }
        try (PreparedStatement p =
            c.prepareStatement(
                "INSERT INTO demo_order_items(order_id,name,unit_price,quantity,subtotal) VALUES(?,?,?,?,?)")) {
          for (Map<String, Object> row : rows) {
            p.setLong(1, orderId);
            p.setString(2, (String) row.get("name"));
            p.setBigDecimal(3, (BigDecimal) row.get("price"));
            p.setInt(4, (Integer) row.get("quantity"));
            p.setBigDecimal(5, (BigDecimal) row.get("subtotal"));
            p.addBatch();
          }
          p.executeBatch();
        }
        c.commit();
        return orderId;
      } catch (SQLException | RuntimeException e) {
        c.rollback();
        throw e;
      }
    }
  }

  public static List<Map<String, Object>> orders(long userId) throws SQLException {
    try (Connection c = Db.open();
        PreparedStatement p =
            c.prepareStatement(
                "SELECT * FROM demo_orders WHERE account_id=? ORDER BY id DESC LIMIT 100")) {
      p.setLong(1, userId);
      try (ResultSet rs = p.executeQuery()) {
        List<Map<String, Object>> rows = new ArrayList<>();
        while (rs.next()) rows.add(row(rs));
        return rows;
      }
    }
  }

  public static Map<String, Object> order(long orderId, long userId) throws SQLException {
    try (Connection c = Db.open();
        PreparedStatement p =
            c.prepareStatement("SELECT * FROM demo_orders WHERE id=? AND account_id=?")) {
      p.setLong(1, orderId);
      p.setLong(2, userId);
      try (ResultSet rs = p.executeQuery()) {
        if (!rs.next()) return null;
        Map<String, Object> order = row(rs);
        List<Map<String, Object>> items = new ArrayList<>();
        try (PreparedStatement lines =
            c.prepareStatement("SELECT * FROM demo_order_items WHERE order_id=? ORDER BY id")) {
          lines.setLong(1, orderId);
          try (ResultSet ls = lines.executeQuery()) {
            while (ls.next()) items.add(row(ls));
          }
        }
        order.put("items", items);
        return order;
      }
    }
  }

  public static void saveAccount(
      long id, String username, String name, String email, String role, String password)
      throws SQLException {
    if (role == null || !Set.of("admin", "member").contains(role))
      throw new IllegalArgumentException("Peran tidak valid.");
    String hash = password == null || password.isEmpty() ? null : Passwords.hash(password);
    try (Connection c = Db.open()) {
      c.setAutoCommit(false);
      try {
        int admins = 0;
        String oldRole = null;
        try (PreparedStatement p =
                c.prepareStatement("SELECT id,role FROM accounts ORDER BY id FOR UPDATE");
            ResultSet rs = p.executeQuery()) {
          while (rs.next()) {
            if ("admin".equals(rs.getString("role"))) admins++;
            if (rs.getLong("id") == id) oldRole = rs.getString("role");
          }
        }
        if (id != 0 && oldRole == null) throw new IllegalArgumentException("Akun tidak ditemukan.");
        if ("admin".equals(oldRole) && admins <= 1 && !"admin".equals(role))
          throw new IllegalArgumentException("Admin terakhir tidak dapat diturunkan perannya.");
        if (id == 0 && hash == null)
          throw new IllegalArgumentException("Kata sandi wajib untuk akun baru.");
        String sql =
            id == 0
                ? "INSERT INTO accounts(username,name,email,role,password_hash) VALUES(?,?,?,?,?)"
                : hash == null
                    ? "UPDATE accounts SET username=?,name=?,email=?,role=? WHERE id=?"
                    : "UPDATE accounts SET username=?,name=?,email=?,role=?,password_hash=? WHERE id=?";
        try (PreparedStatement p = c.prepareStatement(sql)) {
          p.setString(1, username);
          p.setString(2, name);
          p.setString(3, email);
          p.setString(4, role);
          if (id == 0) {
            p.setString(5, hash);
          } else if (hash == null) {
            p.setLong(5, id);
          } else {
            p.setString(5, hash);
            p.setLong(6, id);
          }
          p.executeUpdate();
        }
        c.commit();
      } catch (SQLException | RuntimeException e) {
        c.rollback();
        throw e;
      }
    }
  }

  public static void deleteAccount(long id) throws SQLException {
    try (Connection c = Db.open()) {
      c.setAutoCommit(false);
      try {
        int admins = 0;
        String role = null;
        try (PreparedStatement p =
                c.prepareStatement("SELECT id,role FROM accounts ORDER BY id FOR UPDATE");
            ResultSet rs = p.executeQuery()) {
          while (rs.next()) {
            if ("admin".equals(rs.getString("role"))) admins++;
            if (rs.getLong("id") == id) role = rs.getString("role");
          }
        }
        if (role == null) throw new IllegalArgumentException("Akun tidak ditemukan.");
        if ("admin".equals(role) && admins <= 1)
          throw new IllegalArgumentException("Admin terakhir tidak dapat dihapus.");
        try (PreparedStatement p = c.prepareStatement("DELETE FROM accounts WHERE id=?")) {
          p.setLong(1, id);
          p.executeUpdate();
        }
        c.commit();
      } catch (SQLException | RuntimeException e) {
        c.rollback();
        throw e;
      }
    }
  }
}

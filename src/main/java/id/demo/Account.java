package id.demo;

public final class Account {
  private final long id;
  private final String username, name, email, role, hash;

  public Account(long id, String username, String name, String email, String role, String hash) {
    this.id = id;
    this.username = username;
    this.name = name;
    this.email = email;
    this.role = role;
    this.hash = hash;
  }

  public long getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  public String getRole() {
    return role;
  }

  public String getHash() {
    return hash;
  }

  public boolean isAdmin() {
    return "admin".equals(role);
  }
}

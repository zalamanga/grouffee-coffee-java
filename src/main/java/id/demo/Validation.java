package id.demo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public final class Validation {
  private Validation() {}

  public static String text(String value, String label, int max) {
    String result = value == null ? "" : value.replace("\r\n", "\n").strip();
    if (result.isEmpty()
        || result.length() > max
        || result.chars().anyMatch(c -> Character.isISOControl(c) && c != '\n')) {
      throw new IllegalArgumentException(label + " wajib diisi, maksimum " + max + " karakter.");
    }
    return result;
  }

  public static String username(String value) {
    String result = text(value, "Username", 40).toLowerCase(java.util.Locale.ROOT);
    if (!result.matches("[a-z0-9_]{4,40}"))
      throw new IllegalArgumentException(
          "Username harus 4 sampai 40 huruf, angka, atau garis bawah.");
    return result;
  }

  public static String email(String value) {
    String result = text(value, "Email", 160).toLowerCase(java.util.Locale.ROOT);
    if (!result.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
      throw new IllegalArgumentException("Masukkan alamat email yang valid.");
    return result;
  }

  public static String password(String value) {
    if (value == null || value.length() < 12 || value.length() > 128)
      throw new IllegalArgumentException("Kata sandi harus 12 sampai 128 karakter.");
    return value;
  }

  public static int number(String value, int max, String label) {
    try {
      if (value == null || !value.matches("[0-9]{1,10}")) throw new NumberFormatException();
      int number = Integer.parseInt(value);
      if (number < 1 || number > max) throw new NumberFormatException();
      return number;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(label + " harus bilangan bulat 1 sampai " + max + ".");
    }
  }

  public static BigDecimal price(String value) {
    try {
      if (value == null || !value.matches("[0-9]{1,7}(\\.[0-9]{1,2})?"))
        throw new NumberFormatException();
      BigDecimal result = new BigDecimal(value).setScale(2);
      if (result.signum() <= 0) throw new NumberFormatException();
      return result;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "Harga harus lebih dari nol, maksimum 9999999.99, dengan paling banyak dua angka desimal.");
    }
  }

  public static String birthDate(String value) {
    try {
      LocalDate date = LocalDate.parse(value == null ? "" : value);
      if (date.isAfter(LocalDate.now()) || date.isBefore(LocalDate.of(1900, 1, 1)))
        throw new DateTimeParseException("Range", value, 0);
      return date.toString();
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException(
          "Tanggal lahir harus valid, dari tahun 1900 sampai hari ini.");
    }
  }
}

package id.demo;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class Passwords {
  private static final int ITERATIONS = 600_000;
  private static final SecureRandom RANDOM = new SecureRandom();

  private Passwords() {}

  public static String hash(String password) {
    Validation.password(password);
    byte[] salt = new byte[16];
    RANDOM.nextBytes(salt);
    return "pbkdf2$"
        + ITERATIONS
        + "$"
        + Base64.getEncoder().encodeToString(salt)
        + "$"
        + Base64.getEncoder().encodeToString(derive(password, salt, ITERATIONS));
  }

  public static boolean verify(String password, String stored) {
    if (password == null || password.length() > 128 || stored == null) return false;
    try {
      String[] fields = stored.split("\\$");
      if (fields.length != 4 || !"pbkdf2".equals(fields[0])) return false;
      int iterations = Integer.parseInt(fields[1]);
      if (iterations != ITERATIONS) return false;
      byte[] salt = Base64.getDecoder().decode(fields[2]);
      byte[] expected = Base64.getDecoder().decode(fields[3]);
      if (salt.length != 16 || expected.length != 32) return false;
      return MessageDigest.isEqual(expected, derive(password, salt, iterations));
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private static byte[] derive(String password, byte[] salt, int iterations) {
    char[] chars = password.toCharArray();
    PBEKeySpec spec = new PBEKeySpec(chars, salt, iterations, 256);
    try {
      return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("PBKDF2 unavailable", e);
    } finally {
      spec.clearPassword();
      Arrays.fill(chars, '\0');
    }
  }
}

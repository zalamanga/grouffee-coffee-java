package id.demo;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.*;
import java.lang.reflect.Proxy;
import java.util.concurrent.*;
import javax.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

class DomainTest {
  @Test
  void everyCartMutationWaitsUntilCheckoutReleasesTheSessionMonitor() throws Exception {
    for (String action : List.of("add", "remove", "clear")) {
      Map<String, Object> attributes = new ConcurrentHashMap<>();
      Cart cart = new Cart();
      cart.add(1, 1);
      attributes.put("cart", cart);
      attributes.put("checkoutToken", "old-token");
      HttpSession session =
          (HttpSession)
              Proxy.newProxyInstance(
                  HttpSession.class.getClassLoader(),
                  new Class<?>[] {HttpSession.class},
                  (proxy, method, args) -> {
                    switch (method.getName()) {
                      case "getAttribute":
                        return attributes.get(args[0]);
                      case "setAttribute":
                        attributes.put((String) args[0], args[1]);
                        return null;
                      case "removeAttribute":
                        attributes.remove(args[0]);
                        return null;
                      default:
                        throw new UnsupportedOperationException(method.getName());
                    }
                  });
      ExecutorService executor = Executors.newSingleThreadExecutor();
      CountDownLatch entered = new CountDownLatch(1);
      try {
        Future<?> mutation;
        synchronized (session) {
          Map<Long, Integer> purchased = cart.snapshot();
          mutation =
              executor.submit(
                  () -> {
                    entered.countDown();
                    AppServlet.updateCart(session, action, 2, 1);
                  });
          assertTrue(entered.await(2, TimeUnit.SECONDS));
          assertThrows(TimeoutException.class, () -> mutation.get(100, TimeUnit.MILLISECONDS));
          assertEquals(Map.of(1L, 1), purchased);
          cart.clear();
        }
        mutation.get(2, TimeUnit.SECONDS);
        assertEquals("add".equals(action) ? Map.of(2L, 1) : Map.of(), cart.snapshot());
        assertFalse(attributes.containsKey("checkoutToken"));
      } finally {
        executor.shutdownNow();
      }
    }
  }

  @Test
  void omittedAdminChoicesAreValidationErrorsBeforeAnyDatabaseWork() {
    assertThrows(
        IllegalArgumentException.class,
        () -> CoffeeRepository.saveMenu(0, "Contoh", null, BigDecimal.ONE, "croissant.png"));
    assertThrows(
        IllegalArgumentException.class,
        () -> CoffeeRepository.saveMenu(0, "Contoh", "Makanan", BigDecimal.ONE, null));
    assertThrows(
        IllegalArgumentException.class,
        () -> CoffeeRepository.saveAccount(0, "demo", "Contoh", "demo@example.test", null, ""));
  }

  @Test
  void passwordHashesAreSaltedAndRejectWrongOrMalformedValues() {
    String first = Passwords.hash("Kopi-demo-password-123");
    String second = Passwords.hash("Kopi-demo-password-123");
    assertNotEquals(first, second);
    assertTrue(Passwords.verify("Kopi-demo-password-123", first));
    assertFalse(Passwords.verify("wrong password", first));
    assertFalse(Passwords.verify(null, first));
    for (String value :
        List.of(
            "plain",
            "pbkdf2$1$AA==$AA==",
            "pbkdf2$600000$invalid$invalid",
            first.replace("600000", "999999999")))
      assertFalse(Passwords.verify("Kopi-demo-password-123", value));
    assertThrows(IllegalArgumentException.class, () -> Passwords.hash("short"));
  }

  @Test
  void cartBoundsCannotBeBypassedByRepeatedAddsAndSnapshotsAreIndependent() {
    Cart cart = new Cart();
    cart.add(1, 98);
    cart.add(1, 1);
    assertEquals(99, cart.snapshot().get(1L));
    assertThrows(IllegalArgumentException.class, () -> cart.add(1, 1));
    assertThrows(IllegalArgumentException.class, () -> cart.add(2, 0));
    assertThrows(IllegalArgumentException.class, () -> cart.add(-1, 1));
    Map<Long, Integer> snapshot = cart.snapshot();
    snapshot.clear();
    assertEquals(1, cart.snapshot().size());
    for (long id = 2; id <= 20; id++) cart.add(id, 1);
    assertThrows(IllegalArgumentException.class, () -> cart.add(21, 1));
    cart.remove(1);
    cart.add(21, 1);
    assertEquals(20, cart.snapshot().size());
    cart.clear();
    assertTrue(cart.snapshot().isEmpty());
  }

  @Test
  void monetaryTotalsRetainDecimalPrecisionAndPriceRejectsExponentsOrOverflow() {
    assertEquals(
        new BigDecimal("0.30"),
        CoffeeRepository.total(
            List.of(
                Map.of("subtotal", new BigDecimal("0.10")),
                Map.of("subtotal", new BigDecimal("0.20")))));
    assertEquals(new BigDecimal("9999999.99"), Validation.price("9999999.99"));
    for (String value : List.of("0", "-1", "1e3", "1.001", "10000000", "NaN", " 2"))
      assertThrows(IllegalArgumentException.class, () -> Validation.price(value));
  }

  @Test
  void identityAndQuantityValidationRejectsAmbiguousOrOversizedValues() {
    assertEquals("demo_user", Validation.username("Demo_User"));
    assertEquals("a@example.test", Validation.email("A@EXAMPLE.TEST"));
    assertEquals(Integer.MAX_VALUE, Validation.number("2147483647", Integer.MAX_VALUE, "ID"));
    for (String value : List.of("0", "-1", "1.1", "1e1", "2147483648", "10000000000"))
      assertThrows(IllegalArgumentException.class, () -> Validation.number(value, 999, "Meja"));
    assertThrows(IllegalArgumentException.class, () -> Validation.username("demo' OR 1=1"));
    assertThrows(IllegalArgumentException.class, () -> Validation.email("a@b"));
    assertEquals(
        "Jalan contoh\nBaris dua", Validation.text("Jalan contoh\r\nBaris dua", "Alamat", 100));
  }

  @Test
  void csrfRequiresFullExactToken() {
    String expected = "a".repeat(43);
    assertTrue(SecurityFilter.matches(expected, expected));
    assertFalse(SecurityFilter.matches(expected, "b" + expected.substring(1)));
    assertFalse(SecurityFilter.matches(expected, "a"));
    assertFalse(SecurityFilter.matches(null, expected));
    assertFalse(SecurityFilter.matches(expected, null));
  }
}

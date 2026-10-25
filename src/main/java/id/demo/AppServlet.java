package id.demo;

import java.io.IOException;
import java.sql.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet(value = "/", loadOnStartup = 1)
public final class AppServlet extends HttpServlet {
  @Override
  public void init() throws ServletException {
    try {
      Accounts.bootstrapAdmin();
    } catch (SQLException | IllegalArgumentException e) {
      throw new ServletException(
          "Admin bootstrap failed; verify demo environment and database.", e);
    }
  }

  private static long id(HttpServletRequest r) {
    return Validation.number(r.getParameter("id"), Integer.MAX_VALUE, "ID");
  }

  private static Account account(HttpServletRequest r) {
    return (Account) r.getAttribute("account");
  }

  private static Cart cart(HttpServletRequest r) {
    return cart(r.getSession());
  }

  private static Cart cart(HttpSession s) {
    synchronized (s) {
      Cart cart = (Cart) s.getAttribute("cart");
      if (cart == null) {
        cart = new Cart();
        s.setAttribute("cart", cart);
      }
      return cart;
    }
  }

  static void updateCart(HttpSession session, String action, long id, int quantity) {
    synchronized (session) {
      Cart current = cart(session);
      switch (action) {
        case "add":
          current.add(id, quantity);
          break;
        case "remove":
          current.remove(id);
          break;
        case "clear":
          current.clear();
          break;
        default:
          throw new IllegalArgumentException("Tindakan keranjang tidak valid.");
      }
      session.removeAttribute("checkoutToken");
    }
  }

  private static boolean needsAccount(String path) {
    return path.startsWith("/admin")
        || Set.of(
                "/cart",
                "/cart/add",
                "/cart/remove",
                "/cart/clear",
                "/checkout",
                "/orders",
                "/order",
                "/profile",
                "/password")
            .contains(path);
  }

  private static void redirect(HttpServletRequest r, HttpServletResponse s, String path)
      throws IOException {
    s.sendRedirect(r.getContextPath() + path);
  }

  private boolean guard(HttpServletRequest r, HttpServletResponse s) throws IOException {
    String path = r.getServletPath();
    Account a = account(r);
    if (needsAccount(path) && a == null) {
      redirect(r, s, "/login");
      return false;
    }
    if (path.startsWith("/admin") && !a.isAdmin()) {
      s.sendError(403);
      return false;
    }
    return true;
  }

  @Override
  protected void doGet(HttpServletRequest r, HttpServletResponse s)
      throws ServletException, IOException {
    if (r.getServletPath().startsWith("/assets/")) {
      r.getServletContext().getNamedDispatcher("default").forward(r, s);
      return;
    }
    if ("/health".equals(r.getServletPath())) {
      try (Connection c = Db.open();
          PreparedStatement p = c.prepareStatement("SELECT 1 FROM menu LIMIT 1")) {
        p.executeQuery().close();
        s.setContentType("text/plain");
        s.getWriter().print("ready");
      } catch (SQLException e) {
        s.setStatus(503);
        s.getWriter().print("unavailable");
      }
      return;
    }
    if (!guard(r, s)) return;
    try {
      render(r, s, r.getServletPath());
    } catch (SQLException e) {
      databaseError(r, s, e);
    } catch (IllegalArgumentException e) {
      s.setStatus(400);
      r.setAttribute("error", e.getMessage());
      view(r, s, "error");
    }
  }

  @Override
  protected void doPost(HttpServletRequest r, HttpServletResponse s)
      throws ServletException, IOException {
    if (!guard(r, s)) return;
    String path = r.getServletPath();
    try {
      Account a = account(r);
      String next = "/";
      switch (path) {
        case "/register":
          String password = Validation.password(r.getParameter("password"));
          if (!password.equals(r.getParameter("confirm")))
            throw new IllegalArgumentException("Konfirmasi kata sandi tidak sama.");
          Accounts.register(
              Validation.username(r.getParameter("username")),
              Validation.text(r.getParameter("name"), "Nama", 100),
              Validation.email(r.getParameter("email")),
              password);
          next = "/login?registered=1";
          break;
        case "/login":
          Account found =
              Accounts.login(
                  Validation.username(r.getParameter("username")), r.getParameter("password"));
          if (found == null)
            throw new IllegalArgumentException("Username atau kata sandi tidak sesuai.");
          r.getSession().invalidate();
          HttpSession fresh = r.getSession(true);
          fresh.setAttribute("accountId", found.getId());
          SecurityFilter.token(fresh);
          next = "/menu";
          break;
        case "/logout":
          r.getSession().invalidate();
          next = "/";
          break;
        case "/cart/add":
          if (CoffeeRepository.item(id(r)) == null)
            throw new IllegalArgumentException("Menu tidak tersedia.");
          updateCart(
              r.getSession(),
              "add",
              id(r),
              Validation.number(r.getParameter("quantity"), 99, "Jumlah"));
          next = "/cart";
          break;
        case "/cart/remove":
          updateCart(r.getSession(), "remove", id(r), 0);
          next = "/cart";
          break;
        case "/cart/clear":
          updateCart(r.getSession(), "clear", 0, 0);
          next = "/cart";
          break;
        case "/checkout":
          int table = Validation.number(r.getParameter("table"), 999, "Nomor meja");
          HttpSession session = r.getSession();
          synchronized (session) {
            String token = (String) session.getAttribute("checkoutToken");
            if (token == null || !token.equals(r.getParameter("checkoutToken")))
              throw new IllegalArgumentException(
                  "Keranjang berubah atau sudah diproses. Buka ulang keranjang.");
            long order = CoffeeRepository.checkout(a.getId(), table, cart(r).snapshot(), token);
            cart(r).clear();
            session.removeAttribute("checkoutToken");
            next = "/order?id=" + order;
          }
          break;
        case "/password":
          Accounts.changePassword(a, r.getParameter("currentPassword"), r.getParameter("password"));
          next = "/profile?saved=1";
          break;
        case "/admin/menu-save":
          CoffeeRepository.saveMenu(
              optionalId(r),
              Validation.text(r.getParameter("name"), "Nama menu", 120),
              r.getParameter("category"),
              Validation.price(r.getParameter("price")),
              r.getParameter("image"));
          next = "/admin?saved=1";
          break;
        case "/admin/account-save":
          CoffeeRepository.saveAccount(
              optionalId(r),
              Validation.username(r.getParameter("username")),
              Validation.text(r.getParameter("name"), "Nama", 100),
              Validation.email(r.getParameter("email")),
              r.getParameter("role"),
              r.getParameter("password"));
          next = "/admin?tab=accounts&saved=1";
          break;
        case "/admin/delete":
          if ("menu".equals(r.getParameter("kind"))) CoffeeRepository.deleteMenu(id(r));
          else if ("account".equals(r.getParameter("kind"))) CoffeeRepository.deleteAccount(id(r));
          else throw new IllegalArgumentException("Jenis data tidak valid.");
          next =
              "/admin?tab="
                  + ("account".equals(r.getParameter("kind")) ? "accounts" : "menu")
                  + "&saved=1";
          break;
        default:
          s.sendError(404);
          return;
      }
      redirect(r, s, next);
    } catch (IllegalArgumentException e) {
      s.setStatus(400);
      r.setAttribute("error", e.getMessage());
      renderAfterError(r, s, path);
    } catch (SQLException e) {
      if ("23000".equals(e.getSQLState())) {
        s.setStatus(409);
        r.setAttribute("error", "Username atau email sudah digunakan. Pilih nilai lain.");
        renderAfterError(r, s, path);
      } else databaseError(r, s, e);
    }
  }

  private static long optionalId(HttpServletRequest r) {
    String value = r.getParameter("id");
    return value == null || value.isEmpty() || "0".equals(value) ? 0 : id(r);
  }

  private void renderAfterError(HttpServletRequest r, HttpServletResponse s, String path)
      throws ServletException, IOException {
    try {
      String target =
          path.equals("/admin/menu-save")
              ? "/admin/menu-edit"
              : path.equals("/admin/account-save")
                  ? "/admin/account-edit"
                  : path.equals("/checkout") || path.startsWith("/cart/")
                      ? "/cart"
                      : path.equals("/password") ? "/profile" : path;
      render(r, s, target);
    } catch (SQLException e) {
      databaseError(r, s, e);
    } catch (IllegalArgumentException e) {
      r.setAttribute("error", e.getMessage());
      view(r, s, "error");
    }
  }

  private void render(HttpServletRequest r, HttpServletResponse s, String path)
      throws SQLException, ServletException, IOException {
    String page;
    switch (path) {
      case "/":
        page = "home";
        break;
      case "/menu":
        r.setAttribute("menu", CoffeeRepository.menu());
        page = "menu";
        break;
      case "/register":
        page = "register";
        break;
      case "/login":
        page = "login";
        break;
      case "/help":
        page = "help";
        break;
      case "/about":
        page = "about";
        break;
      case "/profile":
        page = "profile";
        break;
      case "/cart":
        Map<Long, Integer> snapshot;
        HttpSession cartSession = r.getSession();
        synchronized (cartSession) {
          snapshot = cart(cartSession).snapshot();
          if (cartSession.getAttribute("checkoutToken") == null)
            cartSession.setAttribute("checkoutToken", UUID.randomUUID().toString());
          r.setAttribute("checkoutToken", cartSession.getAttribute("checkoutToken"));
        }
        r.setAttribute("hasCart", !snapshot.isEmpty());
        try {
          List<Map<String, Object>> rows = CoffeeRepository.cartRows(snapshot);
          r.setAttribute("rows", rows);
          r.setAttribute("total", CoffeeRepository.total(rows));
        } catch (IllegalArgumentException e) {
          r.setAttribute("error", e.getMessage());
          r.setAttribute("invalidCart", true);
        }
        page = "cart";
        break;
      case "/orders":
        r.setAttribute("orders", CoffeeRepository.orders(account(r).getId()));
        page = "orders";
        break;
      case "/order":
        Map<String, Object> order = CoffeeRepository.order(id(r), account(r).getId());
        if (order == null) {
          s.sendError(404);
          return;
        }
        r.setAttribute("order", order);
        page = "order";
        break;
      case "/admin":
        r.setAttribute("menu", CoffeeRepository.menu());
        if ("accounts".equals(r.getParameter("tab"))) r.setAttribute("accounts", Accounts.all());
        page = "admin";
        break;
      case "/admin/menu-edit":
        long itemId = optionalId(r);
        if (itemId != 0) {
          Map<String, Object> item = CoffeeRepository.item(itemId);
          if (item == null) {
            s.sendError(404);
            return;
          }
          r.setAttribute("item", item);
        }
        r.setAttribute("images", new TreeSet<>(CoffeeRepository.IMAGES));
        page = "menu-edit";
        break;
      case "/admin/account-edit":
        long accountId = optionalId(r);
        if (accountId != 0) {
          Account edit = Accounts.find(accountId);
          if (edit == null) {
            s.sendError(404);
            return;
          }
          r.setAttribute("editAccount", edit);
        }
        page = "account-edit";
        break;
      case "/admin/delete":
        String kind = r.getParameter("kind");
        if (!Set.of("menu", "account").contains(kind == null ? "" : kind))
          throw new IllegalArgumentException("Jenis data tidak valid.");
        Object selected = "menu".equals(kind) ? CoffeeRepository.item(id(r)) : Accounts.find(id(r));
        if (selected == null) {
          s.sendError(404);
          return;
        }
        r.setAttribute("selected", selected);
        page = "delete";
        break;
      default:
        s.sendError(404);
        return;
    }
    view(r, s, page);
  }

  private void view(HttpServletRequest r, HttpServletResponse s, String page)
      throws ServletException, IOException {
    r.setAttribute("page", page);
    r.getRequestDispatcher("/WEB-INF/views/app.jsp").forward(r, s);
  }

  private void databaseError(HttpServletRequest r, HttpServletResponse s, SQLException e)
      throws ServletException, IOException {
    getServletContext().log("Database request failed, SQL state=" + e.getSQLState());
    s.setStatus(503);
    r.setAttribute(
        "error",
        "Data belum dapat dimuat atau disimpan. Periksa layanan database, lalu coba lagi.");
    view(r, s, "error");
  }
}

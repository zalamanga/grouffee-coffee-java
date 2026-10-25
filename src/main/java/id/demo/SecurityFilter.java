package id.demo;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Base64;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.*;

@WebFilter("/*")
public final class SecurityFilter implements Filter {
  private static final SecureRandom RANDOM = new SecureRandom();

  public static String token(HttpSession session) {
    Object old = session.getAttribute("csrf");
    if (old instanceof String) return (String) old;
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    session.setAttribute("csrf", token);
    return token;
  }

  public static boolean matches(Object expected, String supplied) {
    return expected instanceof String
        && supplied != null
        && supplied.length() == 43
        && MessageDigest.isEqual(
            ((String) expected).getBytes(java.nio.charset.StandardCharsets.UTF_8),
            supplied.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;
    req.setCharacterEncoding("UTF-8");
    res.setCharacterEncoding("UTF-8");
    res.setHeader("X-Content-Type-Options", "nosniff");
    res.setHeader("X-Frame-Options", "DENY");
    res.setHeader("Referrer-Policy", "same-origin");
    res.setHeader(
        "Content-Security-Policy",
        "default-src 'self'; img-src 'self'; style-src 'self'; script-src 'self'; form-action 'self'; frame-ancestors 'none'; base-uri 'none'");
    String path = req.getServletPath();
    String method = req.getMethod();
    if (("GET".equals(method) || "HEAD".equals(method))
        && (path.startsWith("/assets/") || "/health".equals(path))) {
      chain.doFilter(req, res);
      return;
    }
    if (!"GET".equals(method) && !"HEAD".equals(method) && !"POST".equals(method)) {
      res.sendError(405);
      return;
    }
    res.setHeader("Cache-Control", "no-store");
    HttpSession session = req.getSession();
    token(session);
    if ("POST".equals(method) && !matches(session.getAttribute("csrf"), req.getParameter("csrf"))) {
      res.sendError(403, "Sesi formulir tidak sesuai. Muat ulang halaman.");
      return;
    }
    if ("POST".equals(method) && "/logout".equals(path)) {
      chain.doFilter(req, res);
      return;
    }
    Object id = session.getAttribute("accountId");
    if (id instanceof Long) {
      try {
        Account account = Accounts.find((Long) id);
        if (account == null) {
          session.invalidate();
          res.sendRedirect(req.getContextPath() + "/login");
          return;
        }
        req.setAttribute("account", account);
      } catch (SQLException e) {
        req.getServletContext().log("Account lookup failed, SQL state=" + e.getSQLState());
        res.setStatus(503);
        req.setAttribute(
            "error", "Database belum tersedia. Coba muat ulang setelah layanan database siap.");
        req.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(req, res);
        return;
      }
    }
    chain.doFilter(req, res);
  }
}

package id.demo;

import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public final class DriverLifecycle implements ServletContextListener {
  static void loadDriver() throws SQLException {
    try {
      Class.forName("com.mysql.cj.jdbc.Driver", true, DriverLifecycle.class.getClassLoader());
    } catch (ClassNotFoundException e) {
      throw new SQLException("MySQL JDBC driver is missing from the application", e);
    }
  }

  @Override
  public void contextInitialized(ServletContextEvent event) {
    try {
      loadDriver();
    } catch (SQLException e) {
      throw new IllegalStateException("Cannot initialize application JDBC driver", e);
    }
  }

  @Override
  public void contextDestroyed(ServletContextEvent event) {
    AbandonedConnectionCleanupThread.checkedShutdown();
    Enumeration<Driver> drivers = DriverManager.getDrivers();
    while (drivers.hasMoreElements()) {
      Driver driver = drivers.nextElement();
      if (driver.getClass().getClassLoader() == DriverLifecycle.class.getClassLoader()) {
        try {
          DriverManager.deregisterDriver(driver);
        } catch (SQLException e) {
          event.getServletContext().log("Application JDBC driver cleanup failed", e);
        }
      }
    }
  }
}

package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * ─────────────────────────────────────────────────────────────
 *  DBConnection.java  –  MySQL JDBC Connection Utility
 * ─────────────────────────────────────────────────────────────
 *
 *  WHAT THIS CLASS DOES:
 *  ─────────────────────
 *  This is a Singleton utility class. A Singleton means only ONE
 *  connection object is created and reused throughout the app.
 *  This avoids the overhead of opening/closing a new DB connection
 *  on every query — which would be slow and resource-intensive.
 *
 *  HOW JDBC WORKS (quick theory):
 *  ───────────────────────────────
 *  JDBC (Java Database Connectivity) is Java's standard API to
 *  talk to any relational database. The flow is:
 *
 *    1. Load the driver  →  tells Java WHICH database to use
 *    2. getConnection()  →  opens a socket connection to MySQL
 *    3. Use Connection   →  run queries via Statement / PreparedStatement
 *    4. Close Connection →  release resources when done
 *
 *  CONNECTION URL FORMAT:
 *  ──────────────────────
 *  jdbc:mysql://<host>:<port>/<database>?<params>
 *   └─ jdbc       = protocol
 *   └─ mysql      = sub-protocol (which DB driver)
 *   └─ localhost  = DB server address
 *   └─ 3306       = default MySQL port
 *   └─ studentdb  = your database name
 *
 *  SETUP REQUIRED:
 *  ───────────────
 *  1. Install MySQL and start the server
 *  2. Open MySQL Workbench or CLI and run:
 *       CREATE DATABASE studentdb;
 *  3. Update DB_USER and DB_PASS below with your credentials
 *  4. Add mysql-connector-j-<version>.jar to your classpath
 *
 *  COMPILE & RUN:
 *  ──────────────
 *  javac -cp .;mysql-connector-j.jar db/DBConnection.java
 *  java  -cp .;mysql-connector-j.jar Main
 */
public class DBConnection {

    // ── Configuration — change these to match your MySQL setup ──
    private static final String DB_URL  = "jdbc:mysql://localhost:3306/studentdb"
                                        + "?useSSL=false"          // disable SSL warning in dev
                                        + "&allowPublicKeyRetrieval=true"
                                        + "&serverTimezone=UTC";   // avoid timezone errors
    private static final String DB_USER = "root";                  // your MySQL username
    private static final String DB_PASS = "your_password";          // your MySQL password

    // ── Singleton instance ──
    private static Connection connection = null;

    /**
     * Private constructor — prevents anyone from doing: new DBConnection()
     * This enforces the Singleton pattern.
     */
    private DBConnection() {}

    /**
     * Returns the single shared Connection instance.
     *
     * WHY synchronized?
     * If two threads call getConnection() at the exact same time,
     * without 'synchronized' both might create separate connections.
     * 'synchronized' ensures only one thread enters at a time.
     *
     * @return  active MySQL Connection object
     * @throws  SQLException if connection fails
     */
    public static synchronized Connection getConnection() throws SQLException {
        // If no connection exists yet, OR if the existing one has timed out → create a new one
        if (connection == null || connection.isClosed()) {
            try {
                // Step 1: Load the MySQL JDBC driver class into memory
                // With modern JDBC (4.0+) and the JAR on classpath, this is auto-loaded.
                // We still do it explicitly here for clarity/compatibility.
                Class.forName("com.mysql.cj.jdbc.Driver");

                // Step 2: Open the connection
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);

                System.out.println("✔ Database connected successfully!");

            } catch (ClassNotFoundException e) {
                // This happens if mysql-connector-j.jar is NOT on the classpath
                throw new SQLException(
                    "MySQL JDBC Driver not found!\n" +
                    "Fix: Add mysql-connector-j.jar to your classpath.\n" +
                    "Download: https://dev.mysql.com/downloads/connector/j/", e);

            } catch (SQLException e) {
                // This happens if credentials are wrong, MySQL isn't running, etc.
                throw new SQLException(
                    "Could not connect to MySQL!\n" +
                    "Check: Is MySQL running? Are DB_USER/DB_PASS correct?\n" +
                    "Error: " + e.getMessage(), e);
            }
        }
        return connection;
    }

    /**
     * Closes the connection gracefully.
     * Always call this when your application shuts down.
     */
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
                System.out.println("✔ Database connection closed.");
            } catch (SQLException e) {
                System.err.println("Warning: Could not close DB connection – " + e.getMessage());
            }
        }
    }

    /**
     * Quick connectivity test — useful for debugging.
     * Run this first to verify your setup before running the full app.
     */
    public static void main(String[] args) {
        System.out.println("Testing database connection...");
        try {
            Connection con = DBConnection.getConnection();
            if (con != null && !con.isClosed()) {
                System.out.println("✔ Connection test PASSED!");
                System.out.println("  Connected to: " + con.getMetaData().getURL());
                System.out.println("  MySQL version: " + con.getMetaData().getDatabaseProductVersion());
            }
        } catch (SQLException e) {
            System.err.println("✖ Connection test FAILED: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }
}

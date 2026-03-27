package Data;

import Utils.PathUtils;

import java.sql.*;

/**
 * Singleton managing the SQLite connection and schema lifecycle.
 * <p>
 * Schema versioning uses SQLite's built-in PRAGMA user_version so that future
 * field additions can be applied as incremental ALTER TABLE migrations rather
 * than one-off converter scripts.
 */
public class Database {

    private static final int CURRENT_SCHEMA_VERSION = 1;

    private static Database instance;

    private final Connection connection;

    public static Database getInstance() {
        if (instance == null) {
            try {
                instance = new Database();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to open database: " + PathUtils.DB_PATH, e);
            }
        }
        return instance;
    }

    private Database() throws SQLException {
        try {
            PathUtils.createDirectoryIfNecessary(PathUtils.SETTINGS_DIR_PATH);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Settings directory", e);
        }

        connection = DriverManager.getConnection("jdbc:sqlite:" + PathUtils.DB_PATH);

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA journal_mode = WAL");
            stmt.execute("PRAGMA foreign_keys = ON");
        }

        migrateSchema();
    }

    /** Returns the live JDBC connection. Callers must not close it. */
    public Connection getConnection() {
        return connection;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // Schema migration
    // -------------------------------------------------------------------------

    private void migrateSchema() throws SQLException {
        int version;
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA user_version")) {
            version = rs.getInt(1);
        }

        if (version < 1) {
            applyV1(connection);
            setUserVersion(1);
            version = 1;
        }

        // Future: if (version < 2) { applyV2(connection); setUserVersion(2); }
    }

    private void setUserVersion(int v) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA user_version = " + v);
        }
    }

    private static void applyV1(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS invoices (" +
                "    invoice_number TEXT    PRIMARY KEY," +
                "    invoice_date   INTEGER NOT NULL," +
                "    due_date       INTEGER NOT NULL," +
                "    customer_name  TEXT    NOT NULL," +
                "    customer_phone TEXT    NOT NULL," +
                "    customer_email TEXT    NOT NULL," +
                "    credit         REAL    NOT NULL DEFAULT 0," +
                "    paid           INTEGER NOT NULL DEFAULT 0," +
                "    done           INTEGER NOT NULL DEFAULT 0," +
                "    picked_up      INTEGER NOT NULL DEFAULT 0" +
                ")");

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS invoice_items (" +
                "    id             INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    invoice_number TEXT    NOT NULL" +
                "        REFERENCES invoices(invoice_number) ON DELETE CASCADE," +
                "    position       INTEGER NOT NULL," +
                "    name           TEXT    NOT NULL," +
                "    quantity       INTEGER NOT NULL," +
                "    unit_price     REAL    NOT NULL" +
                ")");

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS expenses (" +
                "    id          INTEGER PRIMARY KEY," +
                "    date        INTEGER NOT NULL," +
                "    description TEXT    NOT NULL," +
                "    total       REAL    NOT NULL" +
                ")");

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS address_book (" +
                "    id    INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    name  TEXT NOT NULL," +
                "    phone TEXT NOT NULL," +
                "    email TEXT NOT NULL," +
                "    UNIQUE(name, phone, email)" +
                ")");

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS quick_items (" +
                "    id         INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    category   TEXT    NOT NULL," +
                "    position   INTEGER NOT NULL," +
                "    name       TEXT    NOT NULL," +
                "    unit_price REAL    NOT NULL" +
                ")");

            // Generic key-value store for lightweight settings (e.g. filter state)
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS settings (" +
                "    key   TEXT PRIMARY KEY," +
                "    value TEXT NOT NULL" +
                ")");
        }
    }
}
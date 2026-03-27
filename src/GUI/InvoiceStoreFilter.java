package GUI;

import Data.Database;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.*;

public class InvoiceStoreFilter implements Serializable {
    private static final long serialVersionUID = 1L;

    public final String customerText;
    public final boolean showNotDoneOnly;
    public final boolean hideDryCleanOnly;
    public final int dueDateSelectionIndex;
    public final int inDateSelectionIndex;

    public InvoiceStoreFilter(String customerText, boolean showNotDoneOnly, boolean hideDryCleanOnly, int dueDateSelectionIndex, int inDateSelectionIndex) {
        this.customerText = customerText;
        this.showNotDoneOnly = showNotDoneOnly;
        this.hideDryCleanOnly = hideDryCleanOnly;
        this.dueDateSelectionIndex = dueDateSelectionIndex;
        this.inDateSelectionIndex = inDateSelectionIndex;
    }

    public void serialize(ObjectOutputStream os) throws IOException {
        os.writeUTF(customerText);
        os.writeBoolean(showNotDoneOnly);
        os.writeBoolean(hideDryCleanOnly);
        os.writeInt(dueDateSelectionIndex);
        os.writeInt(inDateSelectionIndex);
    }

    /** Returns the saved filter, or null if none has been saved yet. */
    public static InvoiceStoreFilter load() {
        try {
            Connection conn = Database.getInstance().getConnection();
            String customerText         = getSetting(conn, "filter.customerText");
            String showNotDoneOnlyStr   = getSetting(conn, "filter.showNotDoneOnly");
            if (customerText == null || showNotDoneOnlyStr == null) return null;
            return new InvoiceStoreFilter(
                    customerText,
                    Boolean.parseBoolean(showNotDoneOnlyStr),
                    Boolean.parseBoolean(getSetting(conn, "filter.hideDryCleanOnly")),
                    Integer.parseInt(getSetting(conn, "filter.dueDateSelectionIndex")),
                    Integer.parseInt(getSetting(conn, "filter.inDateSelectionIndex")));
        } catch (Exception e) {
            return null;
        }
    }

    public void save() {
        Connection conn = Database.getInstance().getConnection();
        try {
            setSetting(conn, "filter.customerText",         customerText);
            setSetting(conn, "filter.showNotDoneOnly",      String.valueOf(showNotDoneOnly));
            setSetting(conn, "filter.hideDryCleanOnly",     String.valueOf(hideDryCleanOnly));
            setSetting(conn, "filter.dueDateSelectionIndex",String.valueOf(dueDateSelectionIndex));
            setSetting(conn, "filter.inDateSelectionIndex", String.valueOf(inDateSelectionIndex));
        } catch (SQLException e) {
            System.err.println("Failed saving filter: " + e.getMessage());
        }
    }

    private static String getSetting(Connection conn, String key) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT value FROM settings WHERE key = ?")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("value") : null;
            }
        }
    }

    private static void setSetting(Connection conn, String key, String value) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR REPLACE INTO settings (key, value) VALUES (?,?)")) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        }
    }

    public static InvoiceStoreFilter deserialize(ObjectInputStream is) throws IOException {
        String customerText = is.readUTF();
        boolean showNotDoneOnly = is.readBoolean();
        boolean hideDryCleanOnly = is.readBoolean();
        int dueDateSelectionIndex = is.readInt();
        int inDateSelectionIndex = is.readInt();

        return new InvoiceStoreFilter(customerText, showNotDoneOnly, hideDryCleanOnly, dueDateSelectionIndex, inDateSelectionIndex);
    }

}

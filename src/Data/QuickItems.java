package Data;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuickItems implements Serializable {

    private static final long serialVersionUID = 1L;

    private final ObservableList<Item> items;

    public QuickItems(List<Item> items) {
        this.items = FXCollections.observableArrayList(items);
    }

    public ObservableList<Item> getItems() {
        return items;
    }

    public void serialize(ObjectOutputStream os) throws IOException {
        os.writeInt(items.size());
        for (Item item : items) {
            os.writeUTF(item.getName());
            // do not need to write quantity - quantity should always be 1
            os.writeDouble(item.getUnitPrice());
        }
    }

    public static QuickItems load(String category) {
        Connection conn = Database.getInstance().getConnection();
        List<Item> loaded = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT name, unit_price FROM quick_items WHERE category = ? ORDER BY position")) {
            ps.setString(1, category);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    loaded.add(new Item(rs.getString("name"), 1, rs.getDouble("unit_price")));
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed loading quick items '" + category + "': " + e.getMessage());
        }
        return new QuickItems(loaded);
    }

    public void save(String category) {
        Connection conn = Database.getInstance().getConnection();
        try {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM quick_items WHERE category = ?")) {
                    ps.setString(1, category);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO quick_items (category, position, name, unit_price) VALUES (?,?,?,?)")) {
                    for (int i = 0; i < items.size(); i++) {
                        ps.setString(1, category);
                        ps.setInt(2, i);
                        ps.setString(3, items.get(i).getName());
                        ps.setDouble(4, items.get(i).getUnitPrice());
                        ps.executeUpdate();
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Failed saving quick items '" + category + "': " + e.getMessage());
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    public static QuickItems deserialize(ObjectInputStream is) throws IOException {
        final int numItems = is.readInt();
        List<Item> items = new ArrayList<>(numItems);
        for (int i = 0; i < numItems; i++) {
            final String name = is.readUTF();
            final double unitPrice = is.readDouble();
            items.add(new Item(name, 1, unitPrice));
        }
        return new QuickItems(items);
    }
}

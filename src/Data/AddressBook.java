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

public class AddressBook implements Serializable {

    private static final long serialVersionUID = 1L;

    private final ObservableList<CustomerInfo> entries;

    public AddressBook(List<CustomerInfo> startingEntries) {
        entries = FXCollections.observableArrayList(startingEntries);
    }

    public boolean add(CustomerInfo customerInfo) {
        boolean added = !entries.contains(customerInfo) && entries.add(customerInfo);
        if (added) {
            FXCollections.sort(entries);
        }
        return added;
    }

    public boolean remove(CustomerInfo customerInfo) {
        return entries.remove(customerInfo);
    }

    public ObservableList<CustomerInfo> getEntries() {
        return entries;
    }

    public void serialize(ObjectOutputStream os) throws IOException {
        os.writeInt(entries.size());
        for (CustomerInfo entry : entries) {
            os.writeUTF(entry.getName());
            os.writeUTF(entry.getPhone());
            os.writeUTF(entry.getEmail());
        }
    }

    public static AddressBook load() {
        Connection conn = Database.getInstance().getConnection();
        List<CustomerInfo> entries = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT name, phone, email FROM address_book ORDER BY name")) {
            while (rs.next()) {
                entries.add(new CustomerInfo(
                        rs.getString("name"),
                        rs.getString("phone"),
                        rs.getString("email")));
            }
        } catch (SQLException e) {
            System.err.println("Failed loading address book: " + e.getMessage());
        }
        return new AddressBook(entries);
    }

    public void save() {
        Connection conn = Database.getInstance().getConnection();
        try {
            conn.setAutoCommit(false);
            try {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("DELETE FROM address_book");
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO address_book (name, phone, email) VALUES (?,?,?)")) {
                    for (CustomerInfo entry : entries) {
                        ps.setString(1, entry.getName());
                        ps.setString(2, entry.getPhone());
                        ps.setString(3, entry.getEmail());
                        ps.executeUpdate();
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Failed saving address book: " + e.getMessage());
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    public static AddressBook deserialize(ObjectInputStream is) throws IOException {
        final int numEntries = is.readInt();
        List<CustomerInfo> entries = new ArrayList<>();
        for (int i = 0; i < numEntries; i++) {
            final String name = is.readUTF();
            final String phone = is.readUTF();
            final String email = is.readUTF();
            entries.add(new CustomerInfo(name, phone, email));
        }
        return new AddressBook(entries);
    }

}

package Data;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class InvoiceStore {

    public static InvoiceStore getInstance() {
        if (instance == null) {
            instance = new InvoiceStore();
        }
        return instance;
    }

    private static InvoiceStore instance;

    private final Map<String, Invoice> invoiceMap;

    private InvoiceStore() {
        invoiceMap = new TreeMap<>();
        loadAll();
    }

    private void loadAll() {
        Connection conn = Database.getInstance().getConnection();
        try {
            // Load all items in one query, grouped by invoice number
            Map<String, List<Item>> itemsByInvoice = new HashMap<>();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                     "SELECT invoice_number, name, quantity, unit_price " +
                     "FROM invoice_items ORDER BY invoice_number, position")) {
                while (rs.next()) {
                    String num = rs.getString("invoice_number");
                    Item item = new Item(rs.getString("name"), rs.getInt("quantity"), rs.getDouble("unit_price"));
                    itemsByInvoice.computeIfAbsent(num, k -> new ArrayList<>()).add(item);
                }
            }

            // Load all invoices and join with items
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM invoices")) {
                while (rs.next()) {
                    String num         = rs.getString("invoice_number");
                    LocalDate invDate  = LocalDate.ofEpochDay(rs.getLong("invoice_date"));
                    LocalDate dueDate  = LocalDate.ofEpochDay(rs.getLong("due_date"));
                    CustomerInfo ci    = new CustomerInfo(
                            rs.getString("customer_name"),
                            rs.getString("customer_phone"),
                            rs.getString("customer_email"));
                    double credit      = rs.getDouble("credit");
                    boolean paid       = rs.getBoolean("paid");
                    boolean done       = rs.getBoolean("done");
                    boolean pickedUp   = rs.getBoolean("picked_up");
                    List<Item> items   = itemsByInvoice.getOrDefault(num, Collections.emptyList());
                    invoiceMap.put(num, new Invoice(num, invDate, dueDate, ci, items, credit, paid, done, pickedUp));
                }
            }

            System.out.println("Loaded " + invoiceMap.size() + " invoices.");
        } catch (SQLException e) {
            System.err.println("Failed loading invoices: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Invoice get(String invoiceNumber) {
        return invoiceMap.get(invoiceNumber);
    }

    public boolean contains(String invoiceNumber) {
        return invoiceMap.containsKey(invoiceNumber);
    }

    public void save(Invoice invoice) {
        final Invoice copy = invoice.copy();
        invoiceMap.put(copy.getInvoiceNumber(), copy);

        Connection conn = Database.getInstance().getConnection();
        try {
            conn.setAutoCommit(false);
            try {
                // Upsert invoice row
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT OR REPLACE INTO invoices " +
                        "(invoice_number, invoice_date, due_date, customer_name, customer_phone, customer_email, credit, paid, done, picked_up) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?)")) {
                    ps.setString(1, copy.getInvoiceNumber());
                    ps.setLong(2, copy.getInvoiceDate().toEpochDay());
                    ps.setLong(3, copy.getDueDate().toEpochDay());
                    ps.setString(4, copy.getCustomerInfo().getName());
                    ps.setString(5, copy.getCustomerInfo().getPhone());
                    ps.setString(6, copy.getCustomerInfo().getEmail());
                    ps.setDouble(7, copy.getCredit());
                    ps.setInt(8, copy.getPaid() ? 1 : 0);
                    ps.setInt(9, copy.getDone() ? 1 : 0);
                    ps.setInt(10, copy.getPickedUp() ? 1 : 0);
                    ps.executeUpdate();
                }

                // Replace items (delete then insert preserves order)
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM invoice_items WHERE invoice_number = ?")) {
                    ps.setString(1, copy.getInvoiceNumber());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO invoice_items (invoice_number, position, name, quantity, unit_price) VALUES (?,?,?,?,?)")) {
                    List<Item> items = copy.getItems();
                    for (int i = 0; i < items.size(); i++) {
                        Item item = items.get(i);
                        ps.setString(1, copy.getInvoiceNumber());
                        ps.setInt(2, i);
                        ps.setString(3, item.getName());
                        ps.setInt(4, item.getQuantity());
                        ps.setDouble(5, item.getUnitPrice());
                        ps.executeUpdate();
                    }
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Save invoice failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    public int getSize() {
        return invoiceMap.size();
    }

    public ObservableList<Invoice> all() {
        return FXCollections.observableList(new ArrayList<>(invoiceMap.values()));
    }
}
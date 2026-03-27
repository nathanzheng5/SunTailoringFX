package Data;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ExpenseStore {

    public static ExpenseStore getInstance() {
        if (instance == null) {
            instance = new ExpenseStore();
        }
        return instance;
    }

    private static ExpenseStore instance;

    private final Map<Integer, Expense> expenses;
    private int maxId;

    private ExpenseStore() {
        expenses = new HashMap<>();
        loadAll();
    }

    private void loadAll() {
        Connection conn = Database.getInstance().getConnection();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM expenses")) {
            while (rs.next()) {
                int id          = rs.getInt("id");
                LocalDate date  = LocalDate.ofEpochDay(rs.getLong("date"));
                String desc     = rs.getString("description");
                double total    = rs.getDouble("total");
                Expense expense = new Expense(id, date, desc, total);
                expenses.put(id, expense);
                if (id > maxId) maxId = id;
            }
            System.out.println("Loaded " + expenses.size() + " expenses.");
        } catch (SQLException e) {
            System.err.println("Failed loading expenses: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public ObservableList<Expense> all() {
        return FXCollections.observableList(new ArrayList<>(expenses.values()));
    }

    public void createExpenseAndSave(LocalDate date, String description, double total) {
        int id = ++maxId;
        save(new Expense(id, date, description, total));
    }

    public void duplicateExpenseAndSave(Expense expense) {
        int id = ++maxId;
        save(new Expense(id, expense.getDate(), expense.getDescription() + " (Duplicated)", expense.getTotal()));
    }

    public void save(Expense expense) {
        expenses.put(expense.getId(), expense);
        Connection conn = Database.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR REPLACE INTO expenses (id, date, description, total) VALUES (?,?,?,?)")) {
            ps.setInt(1, expense.getId());
            ps.setLong(2, expense.getDate().toEpochDay());
            ps.setString(3, expense.getDescription());
            ps.setDouble(4, expense.getTotal());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Save expense failed: " + e.getMessage());
        }
    }

    public void delete(Expense expense) {
        expenses.remove(expense.getId());
        Connection conn = Database.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM expenses WHERE id = ?")) {
            ps.setInt(1, expense.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Delete expense failed: " + e.getMessage());
        }
    }
}
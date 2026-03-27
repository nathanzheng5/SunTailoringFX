import Data.*;
import GUI.InvoiceStoreFilter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * One-time migration: reads all legacy .dat files from Save/, Expenses/, and
 * Settings/ and writes them into the SQLite database (Settings/suntailoring.db).
 *
 * Run this ONCE on the machine that has the existing .dat files, then discard
 * the .dat files (or archive them). The app will use SQLite going forward.
 *
 * Usage: run as a Java application (no arguments needed). Make sure the working
 * directory is the same folder you normally launch the app from.
 */
public class MigrateToSqlite {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Sun Tailoring – .dat → SQLite migration ===");

        // Opening Database.getInstance() creates the schema automatically.
        int invoices  = migrateInvoices();
        int expenses  = migrateExpenses();
        int customers = migrateAddressBook();
        int qiTotal   = migrateQuickItems();
        migrateInvoiceStoreFilter();

        System.out.println();
        System.out.println("Migration complete.");
        System.out.println("  Invoices  : " + invoices);
        System.out.println("  Expenses  : " + expenses);
        System.out.println("  Customers : " + customers);
        System.out.println("  Quick items: " + qiTotal);
        System.out.println();
        System.out.println("The database is at: Settings/suntailoring.db");
        System.out.println("You can now archive or delete the Save/, Expenses/ .dat files");
        System.out.println("and the Settings/*.dat files.");
    }

    private static int migrateInvoices() {
        Path saveDir = Paths.get("Save");
        if (!Files.exists(saveDir)) {
            System.out.println("[invoices] Save/ directory not found – skipping.");
            return 0;
        }

        InvoiceStore store = InvoiceStore.getInstance();
        int[] count = {0};
        try {
            Files.walk(saveDir).forEach(file -> {
                if (!file.toString().endsWith(".dat")) return;
                try (ObjectInputStream is = new ObjectInputStream(Files.newInputStream(file))) {
                    Invoice invoice = Invoice.deserialize(is);
                    store.save(invoice);
                    count[0]++;
                } catch (IOException e) {
                    System.err.println("[invoices] Skipping " + file + ": " + e.getMessage());
                }
            });
        } catch (IOException e) {
            System.err.println("[invoices] Walk failed: " + e.getMessage());
        }
        System.out.println("[invoices] Migrated " + count[0] + " invoices.");
        return count[0];
    }

    private static int migrateExpenses() {
        Path expDir = Paths.get("Expenses");
        if (!Files.exists(expDir)) {
            System.out.println("[expenses] Expenses/ directory not found – skipping.");
            return 0;
        }

        ExpenseStore store = ExpenseStore.getInstance();
        int[] count = {0};
        try {
            Files.walk(expDir).forEach(file -> {
                if (!file.toString().endsWith(".dat")) return;
                try (ObjectInputStream is = new ObjectInputStream(Files.newInputStream(file))) {
                    Expense expense = Expense.deserialize(is);
                    store.save(expense);
                    count[0]++;
                } catch (IOException e) {
                    System.err.println("[expenses] Skipping " + file + ": " + e.getMessage());
                }
            });
        } catch (IOException e) {
            System.err.println("[expenses] Walk failed: " + e.getMessage());
        }
        System.out.println("[expenses] Migrated " + count[0] + " expenses.");
        return count[0];
    }

    private static int migrateAddressBook() {
        File datFile = new File("Settings/addressBook.dat");
        if (!datFile.exists()) {
            System.out.println("[address book] Settings/addressBook.dat not found – skipping.");
            return 0;
        }
        try (ObjectInputStream is = new ObjectInputStream(new FileInputStream(datFile))) {
            AddressBook ab = AddressBook.deserialize(is);
            ab.save();
            int count = ab.getEntries().size();
            System.out.println("[address book] Migrated " + count + " customers.");
            return count;
        } catch (Exception e) {
            System.err.println("[address book] Failed: " + e.getMessage());
            return 0;
        }
    }

    private static int migrateQuickItems() {
        String[] categories = {"Jacket", "Pant", "Shirt", "Dry Clean", "Dress", "Other"};
        int total = 0;
        for (String category : categories) {
            String fileName = "Settings/quick" + category.replaceAll(" ", "") + "Settings.dat";
            File datFile = new File(fileName);
            if (!datFile.exists()) continue;
            try (ObjectInputStream is = new ObjectInputStream(new FileInputStream(datFile))) {
                QuickItems qi = QuickItems.deserialize(is);
                qi.save(category);
                total += qi.getItems().size();
                System.out.println("[quick items] Migrated " + qi.getItems().size() + " items for '" + category + "'.");
            } catch (Exception e) {
                System.err.println("[quick items] Failed for '" + category + "': " + e.getMessage());
            }
        }
        return total;
    }

    private static void migrateInvoiceStoreFilter() {
        File datFile = new File("Settings/invoiceStoreFilters.dat");
        if (!datFile.exists()) {
            System.out.println("[filter] Settings/invoiceStoreFilters.dat not found – skipping.");
            return;
        }
        try (ObjectInputStream is = new ObjectInputStream(new FileInputStream(datFile))) {
            InvoiceStoreFilter filter = InvoiceStoreFilter.deserialize(is);
            filter.save();
            System.out.println("[filter] Invoice store filter migrated.");
        } catch (Exception e) {
            System.err.println("[filter] Failed: " + e.getMessage());
        }
    }
}
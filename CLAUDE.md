# SunTailoringFX

A JavaFX desktop application for tailoring business management — invoices, customers, expenses, and financial summaries.

## Tech Stack

- **Language:** Java (JDK 8+)
- **UI Framework:** JavaFX with FXML layouts
- **Build System:** IntelliJ IDEA (no Maven/Gradle — use the `.iml` module file)
- **Testing:** JUnit 4.13.1 + Hamcrest
- **Key Libraries:** `javax.mail` (email), `barbecue` (barcode generation), `org.xerial:sqlite-jdbc` (database)
- **Data Persistence:** SQLite database at `Settings/suntailoring.db` (gitignored)

## Project Structure

```
src/
  Data/       - Core data models (Invoice, Item, CustomerInfo, Expense, Summary, etc.)
  GUI/        - JavaFX controllers (.java) and layouts (.fxml) + CSS stylesheets
  Html/       - HTML generation for invoice printing/email
  Utils/      - Gmail sender, config management, path utilities
  scripts/    - Data migration and fake-data generation scripts
  Assets/     - PNG icons used in the UI
test/
  Data/       - Unit tests for data models
  Utils/      - Unit tests for utilities
```

## Key Files

- `src/GUI/SunTailoringGUI.java` — JavaFX `Application` entry point
- `src/GUI/SunTailoringGUIController.java` — Main window controller (largest file, core logic)
- `src/Data/Invoice.java` — Core invoice entity with JavaFX properties
- `src/Data/Database.java` — SQLite singleton, schema lifecycle and migrations
- `src/Data/InvoiceStore.java` — Invoice persistence (SQLite-backed)
- `src/Utils/GmailSender.java` — Gmail integration for sending invoices
- `src/Html/InvoiceHtml.java` — HTML invoice generation

## Features

- Invoice creation, editing, printing, and email sending
- Customer address book
- Expense tracking
- Financial summaries (2-day, 7-day, 30-day rolling totals)
- Due-date alerts (today/tomorrow, within 3 days)
- Quick preset items (Jackets, Pants, Shirts, Dresses, Dry Cleaning)
- Barcode printing on invoices
- Legacy data migration scripts

## Documentation

- [`docs/theme-switching.md`](docs/theme-switching.md) — How light/dark theme switching works (ThemeManager, CSS architecture, PlantUML diagrams)

## Development Notes

- Run/build through IntelliJ IDEA — no CLI build scripts exist
- Database is at `Settings/suntailoring.db` (gitignored); WAL mode is enabled
- Schema versioning uses `PRAGMA user_version` — add new migrations in `Database.java` as `applyV2`, `applyV3`, etc.
- `src/scripts/MigrateToSqlite.java` was a one-time migration from the old `.dat` files and has already been run
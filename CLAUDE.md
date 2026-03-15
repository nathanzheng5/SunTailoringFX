# SunTailoringFX

A JavaFX desktop application for tailoring business management — invoices, customers, expenses, and financial summaries.

## Tech Stack

- **Language:** Java (JDK 8+)
- **UI Framework:** JavaFX with FXML layouts
- **Build System:** IntelliJ IDEA (no Maven/Gradle — use the `.iml` module file)
- **Testing:** JUnit 4.13.1 + Hamcrest
- **Key Libraries:** `javax.mail` (email), `barbecue` (barcode generation)
- **Data Persistence:** Java binary serialization (`.ser` files in `Save/`, `Expenses/`, `Settings/` directories — gitignored)

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
- `src/Data/InvoiceStore.java` — Serialized invoice persistence
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

## Development Notes

- Run/build through IntelliJ IDEA — no CLI build scripts exist
- Data files are stored in `Save/`, `Expenses/`, and `Settings/` directories (gitignored)
- Use migration scripts in `src/scripts/` when upgrading serialized data formats
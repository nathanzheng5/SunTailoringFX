# SunTailoringFX — Agent Instructions & Project Guide

A JavaFX desktop application for tailoring business management — invoices, customers, expenses, and financial summaries.

## Tech Stack

- **Language:** Java (JDK 8+ / OpenJFX 19)
- **UI Framework:** JavaFX with FXML layouts and CSS themes (Light/Dark mode)
- **Build System:** IntelliJ IDEA project (no Maven or Gradle — configured via `SunTailoringFX.iml`)
- **Runtime Launcher:** `run.bat` (executes `SunTailoringFX.jar` with JavaFX controls, fxml, and web modules)
- **Testing:** JUnit 4.13.1 + Hamcrest
- **Key Libraries:**
  - `org.xerial:sqlite-jdbc` — Embedded SQLite database
  - `javax.mail` — Invoice emailing via Gmail SMTP
  - `net.sourceforge.barbecue:barbecue` — Barcode generation for invoice tracking
  - Google Drive Client API — Cloud backup
- **Data Persistence:** SQLite database stored at `Settings/suntailoring.db` (gitignored, WAL mode enabled)

---

## Project Structure

```
SunTailoringFX/
├── src/
│   ├── Assets/      # Application icons and UI graphics
│   ├── Data/        # Models & Stores (Invoice, CustomerInfo, Expense, Summary, Database, InvoiceStore, etc.)
│   ├── GUI/         # JavaFX Controllers (.java), FXML layouts (.fxml), and CSS stylesheets
│   ├── Html/        # HTML generation for invoice rendering and printing (InvoiceHtml)
│   ├── Utils/       # Utilities: GmailSender, GoogleDriveUploader, PropertiesConfiguration, PathUtils
│   ├── scripts/     # One-off data migration and mock data generation scripts
│   └── META-INF/    # Manifest configurations
├── test/
│   ├── Data/        # Unit tests for data entities and database stores
│   └── Utils/       # Unit tests for utility helper classes
├── docs/
│   ├── tablet-app.md       # Architecture spec for customer-facing kiosk / tablet HTTP integration
│   └── theme-switching.md  # ThemeManager architecture, CSS tokens, and PlantUML diagrams
├── Settings/        # Local SQLite database (suntailoring.db) and configuration files
├── run.bat          # Windows batch script to launch the packaged JAR
├── CLAUDE.md        # Claude-specific instructions entry point
└── AGENTS.md        # Primary universal agent guide (this file)
```

---

## Key Files & Entry Points

- `src/GUI/SunTailoringGUI.java` — JavaFX `Application` entry point.
- `src/GUI/SunTailoringGUIController.java` — Main window controller; manages core invoice UI, item table, and user interactions.
- `src/GUI/ThemeManager.java` — Dynamic theme switcher managing light/dark stylesheets and UI icon updates.
- `src/Data/Invoice.java` — Core invoice entity utilizing JavaFX observable properties for direct UI data-binding.
- `src/Data/Database.java` — SQLite singleton manager handling connection lifecycle, WAL mode, and schema migrations.
- `src/Data/InvoiceStore.java` — SQLite-backed persistence repository for invoices and line items.
- `src/Data/ExpenseStore.java` — Expense tracking and persistence repository.
- `src/Html/InvoiceHtml.java` — Generates formatted HTML receipts for customer printing and emailing.
- `src/Utils/GmailSender.java` — Handles SMTP delivery of invoice receipts to customers.
- `src/Utils/GoogleDriveUploader.java` — Automates database backup to Google Drive.

---

## Core Features

- **Invoice Processing:** Create, edit, print, barcode-scan, and email customer orders.
- **Quick Item Presets:** Fast selection for common tailoring services (Jackets, Pants, Shirts, Dresses, Dry Cleaning).
- **Customer Directory:** Built-in address book with search and customer history.
- **Financial Summaries:** Rolling financial performance metrics (2-day, 7-day, 30-day views) and cash drawer calculator.
- **Due-Date Alerts:** Visual cues for garments due today, tomorrow, or within 3 days.
- **Theme Switching:** Runtime toggling between light and dark themes with persistent user preference.
- **Customer Kiosk (Planned):** Lightweight embedded HTTP server (`docs/tablet-app.md`) for customer self-entry on a tablet.

---

## Development & Agent Guidelines

1. **Building & Running:**
   - Primary development is done in IntelliJ IDEA using the project's `.iml` file.
   - Deployments run via `run.bat` or directly using `java --module-path <PATH_TO_JAVAFX> --add-modules javafx.controls,javafx.fxml,javafx.web -jar SunTailoringFX.jar`.

2. **Database Migrations:**
   - Schema versioning relies on SQLite's `PRAGMA user_version`.
   - Never modify existing schema migration methods in `src/Data/Database.java`.
   - Add incremental migrations as new methods (`applyV2()`, `applyV3()`, etc.) and bump `CURRENT_VERSION`.

3. **JavaFX Thread Safety:**
   - All UI updates originating from background threads (e.g., mail sending, network calls, DB exports) must be dispatched onto the JavaFX Application Thread via `Platform.runLater()`.

4. **Testing:**
   - Tests reside in `test/` using JUnit 4.13.1.
   - Run tests directly within IntelliJ or your IDE test runner.

5. **Agent Instructions:**
    - When changing code, put them into my active changelist, but don't commit them. Let me review
    - For added files, make sure they show as added file in my active changelist. Don't keep it in Unversioned changelist.

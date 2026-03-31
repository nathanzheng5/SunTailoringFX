# Tablet App — Conversation Summary

## Context

Explored options for giving customers a way to review their invoice items and enter their own contact information (name, phone, email) on a tablet, without requiring any scanning or complex setup.

## Options Considered

### Option 1: Android App
- ~80% of the codebase would need to be rewritten
- JavaFX UI, properties/bindings, JDBC, javax.mail, printing — all incompatible with Android
- Estimated effort: **2–4 months**

### Option 2: Full Web App (local server)
- Spring Boot backend reusing most of the existing data layer
- New frontend for all screens (invoice table, expense tracker, address book, etc.)
- Estimated effort: **2–3 months**

### Option 3: Narrow web app (just customer-facing kiosk) ✅ Chosen
- Only builds the one feature actually needed
- Estimated effort: **~2–3 days**

## Chosen Approach: Embed a Tiny HTTP Server in the JavaFX App

No separate project. No new dependencies. Use the JDK's built-in `com.sun.net.httpserver.HttpServer`.

### Flow
1. Staff builds invoice items in the JavaFX app as usual
2. Staff taps a "Show Customer" button and hands tablet to customer
3. Tablet browser (e.g. `http://localhost:8080`) shows invoice items + total
4. Customer types Name, Phone, Email and hits Submit
5. JavaFX app receives it and auto-populates the customer fields via `Platform.runLater()`
6. Staff takes tablet back — invoice is ready to save, print, etc.

## What Needs to Be Built

### 1. `src/Utils/TabletServer.java` (new file)
- `GET /` — serves the tablet HTML page
- `GET /invoice` — returns current invoice items + total as JSON
- `POST /customer` — receives name/phone/email, updates JavaFX UI

### 2. Tablet HTML page (one file, served by the server)
- Read-only table: item name, quantity, price, total
- Customer form: Name, Phone, Email + Submit button
- Plain HTML/CSS, no framework

### 3. Small change in `SunTailoringGUIController.java`
- Start the HTTP server on app launch (or on demand)
- On POST received: `Platform.runLater()` to populate customer fields in the existing UI

## Key Decisions
- **Local server only** — tablet on same Wi-Fi hits `http://localhost:8080`
- **No QR codes** — staff just opens the browser on the tablet and hands it over
- **No new dependencies** — `com.sun.net.httpserver` is built into the JDK
- **No tablet-side printing/summary/settings** — staff handles that in JavaFX as usual

● Saved. Next time you want to pick this up, just say "let's work on the tablet app" and I'll have the full plan ready.


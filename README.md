# NurScanner

NurScanner is an Android receipt scanner for NUR BESHIR UMER receipts that extracts line items with OCR, validates totals, stores results locally, and syncs rows to Google Sheets.

## Main Concept

The app turns receipt photos into structured bookkeeping rows. It captures a receipt with the device camera, preprocesses the image for OCR, parses item names and prices, lets the user review and edit extracted rows, saves them in a Room database, and syncs unsynced rows to a Google Apps Script web app connected to Google Sheets.

## Core Features

- Camera-based receipt capture
- OCR preprocessing and text extraction
- Receipt parser for items, subtotal, 2 percent TOT, and totals
- Review screen with editable item rows
- Duplicate detection
- Local Room database history
- Pending sync count
- Google Apps Script URL configuration
- Test connection and sync to Google Sheets
- Included Apps Script backend in `google_apps_script.js`

## Tech Stack

- Android / Kotlin
- Jetpack Compose
- CameraX
- ML Kit OCR
- Room database
- Retrofit-style remote sync models
- Google Apps Script and Google Sheets

## Run Locally

1. Open the project in Android Studio.
2. Create `.env` from `.env.example` if Gemini/API features are used.
3. Let Gradle sync dependencies.
4. Run on an emulator or Android device with camera access.

## Google Sheets Sync

Deploy `google_apps_script.js` as a Google Apps Script web app, then paste the web app URL into the app settings screen.

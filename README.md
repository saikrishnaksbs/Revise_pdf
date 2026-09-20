# Revise PDF

An offline Android app for revising PDFs paragraph-by-paragraph using active recall,
built for daily use on a phone (developed with an iQOO Neo 9 Pro in mind).

## What it does (V1)

1. **Existing PDF / New PDF choice, every time** — the home (library) screen lists every
   PDF you've already added and lets you add another one at any time.
2. **Page-by-page PDF processing** — when you add a PDF, it's read one page at a time
   (via PdfBox-Android) with a live progress indicator.
3. **Paragraph-by-paragraph extraction** — each page's text is split into paragraphs.
4. **One recall point per paragraph** — every paragraph becomes a recall point: "recall
   this paragraph," reveal to check yourself.
5. **Persistent local storage** — extracted PDFs are hashed (SHA-256) and the extraction
   (pages, paragraphs, recall points) is stored in a local Room database, keyed by that
   hash. Re-adding the same PDF skips reprocessing entirely.
6. **Configurable notification intervals, down to 1 minute** — set in Settings.
7. **Start / pause / resume / stop revision** — a session controls whether reminders are
   scheduled; state survives app restarts and device reboots.
8. **Progress tracking** — due-now count and "reviewed at least once" count per document.

Everything runs fully offline — the app requests no `INTERNET` permission.

## Planned (V2)

`RecallPointGenerator` (in `core`) is the seam for this: a second implementation backed
by a local Qwen-VL model (via an on-device inference runtime) can turn paragraphs,
tables, diagrams, and images into real questions instead of plain "recall this
paragraph" prompts, without changing the data model, the database schema, or any UI
code — it's a drop-in generator.

## Architecture

- **`core/`** — pure Kotlin/JVM module, no Android dependency. Domain models, the
  paragraph splitter, the V1 recall-point generator, and a small leveled
  spaced-repetition scheduler. Has JUnit tests you can run directly with
  `./gradlew :core:test` — no Android SDK required.
- **`app/`** — the Android app: Jetpack Compose UI, Room for storage, PdfBox-Android
  for extraction, DataStore for settings, and `AlarmManager` (`setExactAndAllowWhileIdle`,
  rescheduled on every tick) for reminders, since `WorkManager`'s periodic work can't go
  below 15 minutes and the spec calls for intervals as short as 1 minute.

## Getting an installable APK

This sandbox's network policy blocks `dl.google.com` (the Android SDK and Google's
Maven repository), so the Android app module can't be compiled inside this session —
only the plain-JVM `core` module could be built and tested here (and its tests pass).

Instead, **GitHub Actions builds the APK for you**: `.github/workflows/build-apk.yml`
runs on every push, using a GitHub-hosted runner with full internet access, and
uploads a debug APK as a workflow artifact.

To install it on your phone:

1. Push/merge to this repo (or just wait for the workflow on this branch to finish).
2. Open the **Actions** tab → the latest **Build APK** run → download the
   **revise-pdf-debug-apk** artifact (a zip containing `app-debug.apk`).
3. Copy `app-debug.apk` to your phone and open it. You'll need to allow
   "install from unknown sources" for whichever app you use to open the file.

No Android Studio or SDK setup needed on your end.

### If your phone throttles the reminders

Some vendor Android skins are aggressive about killing scheduled alarms — this is
common on iQOO/vivo (FunTouch OS) devices. If reminders arrive late or not at all:

- Grant "Alarms & reminders" access (Settings screen has a shortcut for this).
- Disable battery optimization for this app.
- Allow the app to auto-start in the background.

## Permissions

- `POST_NOTIFICATIONS` — to show revision reminders (Android 13+).
- `SCHEDULE_EXACT_ALARM` — to fire reminders at the exact configured interval.
- `RECEIVE_BOOT_COMPLETED` — to resume a running session's reminders after a reboot.

No storage or internet permissions are needed: PDFs are picked via the system file
picker and copied into the app's private storage.

## Current development layout

See [the repository guide](docs/REPOSITORY-GUIDE.md) for the file map, editing,
building and debugging. Frontend lives in `app/src/main/assets/web`; native code
is grouped by feature under `app/src/main/java/com/paydaytracker/app`.
Merging into `main` automatically builds a signed APK. Check that commit's successful
**Build Android APK** run when downloading an update.

# Payday Tracker for Android

Payday Tracker records shifts, breaks, gross earnings, estimated net earnings, forecasts, settings, and language choice without an internet connection. Optional Google sign-in uses a network connection when configured; it does not upload financial records.

## Version 2.2.5

Upcoming-shift reminders support all planned shifts or selected shifts, with a lead time in hours or days. Android schedules them locally, including while the app is closed. The euro navigation icon is corrected. Native Google account authentication is implemented but requires the app owner's configuration before it can work: see [Google sign-in setup](docs/GOOGLE-SIGN-IN.md). It does not provide automatic Drive synchronization. See [release notes](VERSION-2.2.5.md).

## Version 1.8.1

Version 1.8.1 replaces browser-generated prompts with branded in-app dialogs, displays a live accumulated break counter while paused, and adds an Android foreground timer notification for background work and break tracking.

Version 1.8 adds a persistent offline time clock. Users can start work, begin and end breaks, finish a timed shift, or discard an accidental session. The timer survives app closure and saves the completed session directly into work hours.

Version 1.7.1 reorganizes the Add shift screen into a compact mobile-first layout. Related time fields now share rows, template management is visually secondary, and the Cancel/Save actions remain accessible while scrolling.

Version 1.7 adds reusable shift templates and bulk editing. Users can save named schedules, apply them while logging shifts, and update the time, break, status, wage, note, or template for multiple selected shifts in one operation.

The Android interface now uses a dedicated slide-out menu inspired by Material navigation drawers. Profile, language, and light/dark/system appearance controls live in the drawer, while the bottom bar has five evenly spaced positions without overlap.

This update fixes the mobile bottom navigation so the floating add button no longer covers Expenses. In shift selection mode, the full row is now tappable, and Deselect all appears whenever at least one shift is selected.

- Tap any exact dates in a month-by-month calendar and add the same shift to all selected dates.
- Long-press a logged shift to enter multi-selection mode and delete selected shifts together.
- Record expenses such as rent, utilities, health insurance, groceries, transport, and custom costs.
- Mark an expense as recurring to add it automatically in each applicable month.
- Set a monthly savings target and track progress after expenses.
- Choose light, dark, or system-default appearance.
- Use a compact month label and a time-based sun or moon greeting icon.
- Export the selected month's shifts through Android's native CSV save dialog.
- Show confirmations, warnings, and deletion prompts in the selected language.

## Privacy and storage

- Internet permission is used by optional native Google authentication; the WebView blocks network loads.
- Records are saved inside the app's private internal Android storage.
- Other ordinary apps cannot read that storage.
- Android automatic cloud backup and device-transfer backup are disabled. Manual file backup/restore is available through Settings.
- Uninstalling the app or clearing its storage removes local records. Exported files in Documents or Drive can be selected to restore them.

## Device optimization

The interface is optimized for tall Xiaomi and Redmi displays, including the 6.83-inch Xiaomi 15T class. It supports safe system bars, portrait and landscape orientation, 320–600 dp phone widths, touch-sized navigation, and HyperOS WebView.

## Build

Open this folder in Android Studio, install Android SDK 35 when prompted, sync Gradle, then choose Build > Build APK(s). The debug APK is produced under app/build/outputs/apk/debug/.

Minimum Android version: Android 8.0 (API 26). Target: Android 15 (API 35).

Net salary values are simplified estimates, not official German payslips. Because the app is offline, annual tax parameters do not update automatically.

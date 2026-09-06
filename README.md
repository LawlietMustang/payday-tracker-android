# Payday Tracker for Android

This fully offline Android edition records shifts, breaks, gross earnings, estimated net earnings, forecasts, settings, and language choice without an internet connection.

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

- The manifest contains no Internet permission.
- Records are saved inside the app's private internal Android storage.
- Other ordinary apps cannot read that storage.
- Cloud backup and device-transfer backup are disabled.
- Uninstalling the app or clearing its storage permanently removes the records.

## Device optimization

The interface is optimized for tall Xiaomi and Redmi displays, including the 6.83-inch Xiaomi 15T class. It supports safe system bars, portrait and landscape orientation, 320–600 dp phone widths, touch-sized navigation, and HyperOS WebView.

## Build

Open this folder in Android Studio, install Android SDK 35 when prompted, sync Gradle, then choose Build > Build APK(s). The debug APK is produced under app/build/outputs/apk/debug/.

Minimum Android version: Android 8.0 (API 26). Target: Android 15 (API 35).

Net salary values are simplified estimates, not official German payslips. Because the app is offline, annual tax parameters do not update automatically.

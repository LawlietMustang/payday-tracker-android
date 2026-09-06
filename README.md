# Payday Tracker for Android

This fully offline Android edition records shifts, breaks, gross earnings, estimated net earnings, forecasts, settings, and language choice without an internet connection.

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

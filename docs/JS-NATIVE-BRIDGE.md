# JavaScript ↔ Android bridge

Generated contract table: run `python3 scripts/check-project.py --write-doc` after changing the bridge.
CI rejects missing callers, stale documentation, and a version without release notes.

Only `bridge/AndroidBridge.kt` exposes annotated methods. The Activity owns UI actions.
Both calling-thread exceptions and posted UI-thread exceptions are guarded. Failure logs
contain only the method and exception class, never arguments, tokens, PINs or backup data.
Read failures return an error JSON object (or an empty consumed month); action failures
notify `window.nativeBridgeError(method)` with a rate-limited translated message.
Feature callbacks (`backupResult`, `csvResult`, reminder results) retain their existing meaning.
No Crashlytics dependency or remote error reporting is added.

`bridgeContract()` is read-only reflection metadata. Tests inspect it without calling
all methods with dummy arguments, which could delete data or launch system dialogs.
Google tokens and app PINs never cross this bridge. Times use local calendar dates;
elapsed timer values are milliseconds, reminder lead values are minutes. JSON payloads
are produced by the caller listed below and validated by the matching native feature.

| Method | Arguments | Return | JavaScript callers |
| --- | --- | --- | --- |
| `addWidget` | `none` | `Unit` | `app/src/main/assets/web/scripts/core/device.js` |
| `autoBackupState` | `none` | `String` | `app/src/main/assets/web/scripts/features/recovery.js` |
| `backgroundSettings` | `none` | `Unit` | `app/src/main/assets/web/scripts/core/device.js`, `app/src/main/assets/web/scripts/features/shift-reminders.js` |
| `bridgeContract` | `none` | `String` | Read-only instrumentation contract |
| `chooseBackupFolder` | `none` | `Unit` | `app/src/main/assets/web/scripts/features/recovery.js` |
| `clearAppData` | `none` | `Unit` | `app/src/main/assets/web/scripts/features/account.js` |
| `consumePayslipReminder` | `none` | `String` | `app/src/main/assets/web/scripts/features/payslip-reminder.js` |
| `deviceSettings` | `none` | `String` | `app/src/main/assets/web/scripts/core/device.js`, `app/src/main/assets/web/scripts/features/onboarding.js`, `app/src/main/assets/web/scripts/features/shift-reminders.js` |
| `disableAutoBackup` | `none` | `Unit` | `app/src/main/assets/web/scripts/features/recovery.js` |
| `exactReminderSettings` | `none` | `Unit` | `app/src/main/assets/web/scripts/core/device.js`, `app/src/main/assets/web/scripts/features/shift-reminders.js` |
| `exportBackup` | `json: String` | `Unit` | `app/src/main/assets/web/scripts/features/recovery.js` |
| `exportCsv` | `base64Data: String, fileName: String` | `Unit` | `app/src/main/assets/web/scripts/core/app.js` |
| `googleAccountState` | `none` | `String` | `app/src/main/assets/web/scripts/features/google-account.js` |
| `googleSignIn` | `none` | `Unit` | `app/src/main/assets/web/scripts/features/google-account.js` |
| `googleSignOut` | `none` | `Unit` | `app/src/main/assets/web/scripts/features/google-account.js` |
| `importBackup` | `none` | `Unit` | `app/src/main/assets/web/scripts/features/onboarding.js`, `app/src/main/assets/web/scripts/features/recovery.js` |
| `notificationSettings` | `none` | `Unit` | `app/src/main/assets/web/scripts/core/device.js`, `app/src/main/assets/web/scripts/features/shift-reminders.js` |
| `openHome` | `none` | `Unit` | `app/src/main/assets/web/scripts/features/recovery.js` |
| `queueAutoBackup` | `document: String, hash: String` | `Unit` | `app/src/main/assets/web/scripts/features/recovery.js` |
| `requestShiftNotifications` | `none` | `Unit` | `app/src/main/assets/web/scripts/features/onboarding.js`, `app/src/main/assets/web/scripts/features/payslip-reminder.js`, `app/src/main/assets/web/scripts/features/shift-reminders.js` |
| `retryAutoBackup` | `none` | `Unit` | `app/src/main/assets/web/scripts/features/recovery.js` |
| `retryShiftReminders` | `none` | `Unit` | `app/src/main/assets/web/scripts/features/shift-reminders.js` |
| `saveReminder` | `enabled: Boolean, hour: Int, minute: Int, days: Int, language: String` | `Unit` | `app/src/main/assets/web/scripts/core/device.js`, `app/src/main/assets/web/scripts/features/recovery.js`, `app/src/main/assets/web/scripts/features/shift-reminders.js` |
| `setAppLock` | `enabled: Boolean` | `Unit` | `app/src/main/assets/web/scripts/core/device.js` |
| `setDarkMode` | `dark: Boolean` | `Unit` | `app/src/main/assets/web/scripts/core/app.js` |
| `shiftReminderStatus` | `none` | `String` | `app/src/main/assets/web/scripts/features/shift-reminders.js` |
| `stopTimerNotification` | `none` | `Unit` | `app/src/main/assets/web/scripts/core/app.js`, `app/src/main/assets/web/scripts/features/recovery.js` |
| `syncBackupStatus` | `dirty: Boolean, language: String` | `Unit` | `app/src/main/assets/web/scripts/features/recovery.js` |
| `syncPayslipReminder` | `json: String` | `Unit` | `app/src/main/assets/web/scripts/features/payslip-reminder.js` |
| `syncShiftReminders` | `json: String` | `Unit` | `app/src/main/assets/web/scripts/features/shift-reminders.js` |
| `syncWidget` | `state: String, elapsedMs: Double, language: String, progress: String` | `Unit` | `app/src/main/assets/web/scripts/core/device.js` |
| `testReminder` | `none` | `Unit` | `app/src/main/assets/web/scripts/core/device.js` |
| `testShiftReminder` | `none` | `Unit` | `app/src/main/assets/web/scripts/features/shift-reminders.js` |
| `updateTimerNotification` | `state: String, elapsedMs: Double, language: String` | `Unit` | `app/src/main/assets/web/scripts/core/app.js` |

# WageTrack

Track your shifts, working hours, breaks, earnings and expenses in one offline-first Android app. WageTrack was previously named **Payday Tracker**; the repository and release application ID retain their original names so existing installations can continue receiving updates.

**Current version: 2.5.5 · build 46** — [Release notes](VERSION-2.5.5.md)

The visible app uses **native Kotlin and Jetpack Compose**, with Room for local records. A hidden WebView is used only to read legacy localStorage during migration from older versions; it does not render the current interface.

## Features

- **Shifts and workplaces:** multiple workplaces, hourly wages, templates, calendar selection, multi-date entry and batch editing/deletion.
- **Shift status:** planned, completed and cancelled shifts; automatic status selection and optional completion of past planned shifts.
- **Time tracking:** clock-in, work and break timers, and an Android foreground notification for an active session.
- **Time entry:** a draggable analogue clock, editable hour/minute fields, AM/PM selection and 24-hour input. For example, entering `17:30` selects `5:30 PM`.
- **Earnings:** gross pay, estimated net pay, overtime/night/Sunday/holiday bonuses, monthly forecasts, progress and payslip comparisons.
- **Expenses and goals:** recurring expenses, custom categories, budgets, savings goals and contributions.
- **Reminders and security:** upcoming-shift reminders with hours/days of notice, weekday/time reminders, a home-screen widget and optional biometric/PIN lock.
- **Personalisation:** English/German, light/dark/system appearance, currency selection and first-launch setup.
- **Backup and export:** automatic folder backups, JSON import/export and monthly CSV export.

Version 2.5.5 removes the unwanted text “v” markers from selectors, aligns the clock controls, and restores scrolling to the earnings heading when its section is expanded.

## Install or update

1. Open [Build Android APK in GitHub Actions](https://github.com/LawlietMustang/payday-tracker-android/actions/workflows/build-apk.yml).
2. Select a successful run for the intended commit on `main`.
3. Download the **PaydayTracker-release-apk** artifact, extract the ZIP and install `app-release.apk`. GitHub may require sign-in to download artifacts.
4. To confirm the installed version, open the app's side menu and check the version shown at the bottom.

Updates use the release package `com.paydaytracker.app` and the same permanent signing key. Install compatible signed updates over the existing app to retain its local records. Uninstalling or clearing app storage deletes those records; restore an exported backup if reinstalling.

Debug builds use `com.paydaytracker.app.debug`, install separately, and have their own data.

## Build locally

### Requirements

- Git and Android Studio with Android SDK **36**, SDK Build Tools and Platform Tools installed.
- **JDK 17** for Gradle. The repository includes the **Gradle 8.9 wrapper**; a separate Gradle installation is not required.
- Internet access for the initial dependency/SDK downloads. Day-to-day tracking in the installed app works offline.

Open the repository root in Android Studio, select JDK 17 as the Gradle JDK, and sync the project. Android Studio can create `local.properties` with your SDK location; keep that machine-specific file out of Git.

### Clone or update with Git Bash

For a new clone:

```bash
git clone https://github.com/LawlietMustang/payday-tracker-android.git
cd payday-tracker-android
```

To update an existing clone, open Git Bash inside its folder and run:

```bash
git status
git switch main
git pull --ff-only origin main
```

If Git reports local changes or a diverged branch, resolve that before continuing. Do not discard your edits to force an update.

### Build a debug APK

From Git Bash, macOS or Linux:

```bash
./gradlew assembleDebug
```

From Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`.

To build and install on a connected emulator or a phone with USB debugging enabled:

```bash
./gradlew installDebug
```

### Build a signed release APK

Use the existing permanent signing key. See [Signing setup](docs/SIGNING.md) for the repository's one-time setup and key retention requirements.

For a local release build, provide `PAYDAY_KEYSTORE_PATH` and `PAYDAY_KEYSTORE_PASSWORD` in your environment, using the existing keystore with alias `payday`, then run:

```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`.

The GitHub Actions build uses the repository secrets `PAYDAY_KEYSTORE_BASE64`, `PAYDAY_KEYSTORE_PASSWORD` and `PAYDAY_CERT_SHA256`. It verifies the release certificate and stops if signing is missing or inconsistent. Never commit the private key or password, or generate a replacement key for an ordinary update.

## Where to edit the app

The active screen implementation is in **`app/src/main/java/com/paydaytracker/app/ui/parity/`**. Start there when changing the current layout. The older web files in `app/src/main/assets/` are retained for legacy compatibility/reference and tests; editing their CSS or HTML does not change the visible native screens.

Paths below are relative to `app/src/main/java/com/paydaytracker/app/`:

| File or folder | Responsibility |
| --- | --- |
| `MainActivity.kt` | Android entry point, lifecycle and app-lock integration |
| `ui/MainApp.kt` | Navigation, drawer, header and bottom bar |
| `ui/MainViewModel.kt` | Screen state and repository actions |
| `ui/parity/Components.kt` | Shared cards, buttons, selectors, spacing and icons |
| `ui/parity/Overview.kt` | Dashboard, earnings, progress and live time clock |
| `ui/parity/Hours.kt` and `ShiftCalendar.kt` | Calendar and shift browsing/date selection |
| `ui/parity/ShiftEditor.kt` and `BulkShiftEditor.kt` | Single-shift and bulk entry/editing |
| `ui/parity/ShiftPickers.kt` | Clock dial, editable time fields, AM/PM and picker dimensions |
| `ui/parity/Records.kt` | Expenses, savings goals, budgets and history |
| `ui/parity/Preferences.kt` and `Onboarding.kt` | Settings/profile and initial setup |
| `ui/parity/DevicePage.kt` | Reminder, widget and app-lock controls |
| `ui/parity/BackupPage.kt` | Backup, restore, account and CSV controls |
| `ui/theme/` | Compose theme colours and typography |
| `data/PayrollCalculator.kt` | Earnings, bonuses and net-pay estimates |
| `data/WageRepository.kt` and `data/db/` | Record operations, backup data and Room persistence |
| `data/DataMigration.kt` | Migration of older saved app data |
| `NativeCoordinator.kt` | Connects saved state to Android services |
| `ShiftReminders.kt` and `ReminderReceiver.kt` | Android reminder scheduling and delivery |
| `AutoBackup.kt` | Automatic backup queue and file rotation |
| `AppLock.kt`, `AppPin.kt`, `PaydayWidget.kt`, `TimerNotificationService.kt` | Native lock, PIN, widget and timer services |

App icons and other Android resources live in `app/src/main/res/`. Build settings, SDK levels, `versionName` and `versionCode` are in `app/build.gradle.kts`.

After a native UI change, save the file and run/build the app again. In Android Studio, use an emulator or connected phone and Logcat to inspect behaviour and errors.

## Tests and debugging

Local compilation and unit tests:

```bash
./gradlew assembleDebug testDebugUnitTest
```

Native UI and migration tests, with an emulator or device connected:

```bash
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.paydaytracker.app.DeviceSmoke,com.paydaytracker.app.UpgradeSmoke
```

Additional project/resource checks:

```bash
python3 scripts/check-project.py
python3 scripts/embed-icon-masks.py --check
./gradlew -PresourceAudit :app:lintDebug
```

On Windows, use `python` instead of `python3` if that is how Python is installed. In PowerShell, use `.\gradlew.bat` in place of `./gradlew`.

- **Validate Android and UI:** compilation, unit tests, resource checks and retained web-asset tests.
- **Android device smoke test:** native interaction assertions, migration checks, screenshots and Logcat on an API 35 emulator.
- **Build Android APK:** release checks, permanent signing and the downloadable APK artifact.

The v2.5.5 native suite passed all eight tests, including clock dragging/alignment, typed time validation, earnings scrolling, persistence and migration. Physical-device checks are still needed for manufacturer battery restrictions, notification delivery, biometric hardware, launcher widget pinning and configured Google sign-in. See [Device tests](docs/DEVICE-TESTS.md).

## Backup, privacy and Google sign-in

Records are stored in the app's private internal storage. Android's automatic cloud backup/device-transfer backup is disabled in the manifest.

For automatic backups, choose a writable folder once in **Backup & restore**. Subsequent changes update `WageTrack-backup.json` and retain one previous copy, `WageTrack-previous.json`, instead of creating a new dated file for every edit. Keep the selected folder accessible and check the backup status after changes.

You can also export or restore a JSON file manually. Google Drive can be selected through Android's file picker when the Drive provider is available. After reinstalling, choose the saved file to restore your records and configure the backup folder again; unattended recovery from a connected Google account is not implemented.

Google sign-in is optional and requires the owner's Firebase/Google configuration. Signing in alone does not upload financial records or enable Drive synchronisation. See [Google sign-in setup](docs/GOOGLE-SIGN-IN.md). File/folder backups work without app sign-in.

The `INTERNET` permission supports optional authentication; network-dependent account or cloud-file operations require a connection.

## Calculation and compatibility notes

- Minimum Android API: **26**. Compile and target API: **36**.
- German net-pay figures and forecasts are simplified estimates, not official payroll calculations or payslips. Tax parameters are bundled and do not update automatically while offline.
- Currency selection controls monetary display; it does not convert existing amounts or exchange rates.
- Planned-shift completion is an automatic status rule, not proof that the shift was worked. Review completed/cancelled records for accurate totals.

## Further documentation

- [Current release notes](VERSION-2.5.5.md)
- [Native layout and feature parity](docs/NATIVE-PARITY.md)
- [Native device tests](docs/DEVICE-TESTS.md)
- [Permanent signing](docs/SIGNING.md)
- [Google sign-in configuration](docs/GOOGLE-SIGN-IN.md)

Older `VERSION-*.md` files and documents under `docs/` record historical releases; use the current source and this README for the active app architecture and build requirements.

## Legal documents (draft integration)

Privacy Policy and Terms are available offline in Settings and from a themed first-launch agreement. Acceptance is stored on this installation, outside exported backups. English and German texts live in `app/src/main/assets/legal/`.

**These documents are review drafts, not publication-ready policies.** Publisher/contact details, distribution decisions, public URLs and Firebase arrangements must be completed before release. See the [legal release review](docs/legal/RELEASE-REVIEW.md) for the exact outstanding facts and validation steps. The first-launch button acknowledges privacy; it does not grant optional permissions or blanket data consent.

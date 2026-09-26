# Working on WageTrack

## Frontend (what appears inside the app)

- `app/src/main/assets/web/index.html`: screens, dialogs and script load order.
- `web/styles/design.css`: all colors, spacing, responsive layout and icon masks.
- `web/scripts/core/app.js`: saved records, main calculations, rendering and translation.
- `web/scripts/core/menu.js`: shared drawer/Settings routes and labels.
- `web/scripts/core/navigation.js`: Android Back behavior and page history.
- `web/scripts/core/device.js`: native settings and common device feedback.
- `web/scripts/features/`: savings, workplaces/planning, shifts, reminders, payslips,
  accounts, backups and first-run setup. Each filename names the feature it owns.
- `web/scripts/design/design.js`: dashboard arrangement and calendar interactions.
- `web/icons/`: original SVG sources. After editing one, run
  `python3 scripts/embed-icon-masks.py` to regenerate embedded masks.
- `design/unused/`: retained artwork for future features; not included in the APK.

## Native Android code

Under `app/src/main/java/com/paydaytracker/app/`:

| Folder | Responsibility |
| --- | --- |
| `bridge` | Annotated JavaScript interface, documented in JS-NATIVE-BRIDGE.md |
| `reminders` | Work-hour, shift, backup and payslip scheduling/notifications |
| `auth` | Optional Google authentication and securely stored app PIN |
| `widget` | Android home-screen widget |
| `service` | Active timer foreground notification |
| `ui` | Activity, native lock screens and system pickers |
| `util` | Automatic backup storage, cache versioning and local bridge diagnostics |

`LegacyComponents.kt` intentionally preserves the old Android component names.
Do not remove these small entry points: Android stores them in existing widgets,
notification actions and alarms. Native resources stay under `app/src/main/res`.
This is an offline frontend plus native shell, not a server backend.

## Build and test

1. Open the repository root in Android Studio; use JDK 17 and Gradle 8.9.
2. Run `gradle assembleDebug` for a test APK. It uses the separate `.debug` package.
3. Run `python3 scripts/check-project.py` and
   `python3 scripts/embed-icon-masks.py --check` before pushing.
4. With Playwright 1.58.2 and Chromium installed, run `node tests/planning.cjs`.
5. Run `gradle -PresourceAudit :app:lintDebug` for the unused-resource audit.
6. Push a branch and open a PR. CI compiles, checks layouts, runs native tests, and
   installs over a seeded 2.4.10 app to check stored records and an existing widget.
7. A merge into `main` builds the signed APK automatically. Download the APK artifact
   from that commit's successful **Build Android APK** run.

The repo uses installed Gradle; it does not contain a Gradle wrapper, so `./gradlew`
is not a valid command here. Permanent signing instructions are in SIGNING.md.
Google config stays at `app/src/release/assets/google-services.json` (debug has its
own variant). It is intentionally outside `web/` and excluded from Git.

`prepareWebAssets` copies source assets into `app/build/generated/webAssets` and
adds the Gradle version to CSS/script URLs. Edit source files, never generated files.
For browser previews, serve `app/src/main/assets/web` as the document root.
For Android logs, filter Logcat by `WageTrackBridge`. Release notes belong in the
same change as the version bump. CI rejects missing notes or a stale bridge table.

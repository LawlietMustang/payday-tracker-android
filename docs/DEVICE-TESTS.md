# Native device smoke tests

The application renders Compose screens. `DeviceSmoke` uses AndroidJUnit4 and
`createAndroidComposeRule<MainActivity>()`. The migration test alone uses a hidden
WebView to seed the legacy localStorage origin; it does not render the old app.

With JDK 17, Gradle 8.9 and an API 35 emulator:

```bash
gradle connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.paydaytracker.app.DeviceSmoke,com.paydaytracker.app.UpgradeSmoke
```

The suite checks:
- Overview, Hours, Expenses, History and Settings navigation; Android Back.
- Four-step onboarding, visible restore action, country/tax selection and saved profile.
- Native month picker cancellation preserving the selected month.
- Reminders, App lock and Widget tabs remaining available.
- Native shift entry, paid minutes and persistence after activity recreation.
- Populated earnings and goals; running clock surviving activity recreation.
- Backup import/export preserving templates, budgets, profile extras and reminder settings.
- Rejection of invalid backups without inserting malformed records.
- Actual file-origin legacy localStorage migration into Room.

JUnit assertions determine success. CI captures native screenshots with Android
`screencap`, verifies they are nonempty, and uploads them with test reports and Logcat.
The tests use an isolated debug package and synthetic data.

These checks do not replace physical-device testing of biometric hardware,
manufacturer battery restrictions, launcher widget pinning, or Google sign-in
configuration. They also do not simulate a signed APK upgrade over a real user's
installed v2.4.10 database.

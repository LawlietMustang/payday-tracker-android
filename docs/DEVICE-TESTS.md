# Native device smoke tests

The application now renders Compose screens. DeviceSmoke uses AndroidJUnit4 and
createAndroidComposeRule<MainActivity>(), not WebView queries or Android bridge calls.
The Gradle instrumentation runner is androidx.test.runner.AndroidJUnitRunner.

Run on a fresh debug emulator with JDK 17:

```bash
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.paydaytracker.app.DeviceSmoke
```

The tests cover navigation, profile Back, saving a shift through the UI (including
paid minutes), persistence through activity recreation, and cancelling native PIN
setup. Assertions and JUnit XML determine success; printing a PASS marker is insufficient.
CI uploads the HTML/XML test reports and Logcat, including on failure.

The existing UpgradeSmoke file is a placeholder which only prints success messages;
it is deliberately not part of this targeted device run. It does not verify data
migration. A real WebView-to-Room migration test and renewed notification/widget/
backup coverage are still required before treating the native rewrite as release-ready.
These smoke tests do not claim to cover those separate features.

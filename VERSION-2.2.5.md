# Payday Tracker 2.2.5

- Corrected the euro icon's curved stroke and crossbars while retaining the navigation style and 24px icon dimensions.
- Reminders → Upcoming shifts: enable reminders, choose all upcoming planned shifts or selected shifts, and enter a whole number of hours (1–720) or days (1–30) before starting. One day is 24 hours. New planned shifts are automatically included in All mode.
- Native Android scheduling persists across backgrounding, process loss and reboot. Editing/deleting/completing shifts resynchronizes the schedule. Timezone and clock changes recalculate local shift times. A shift added within its reminder window generates one immediate reminder; past shifts are skipped. App lock hides workplace/time details in the notification.
- Existing weekday logging reminders remain separate. Notification permission is required; exact-alarm permission improves timing, and Xiaomi battery settings can still delay delivery.
- Added real native Google Credential Manager and Firebase authentication, including sign-out and error handling. **Google project configuration is still required**: follow docs/GOOGLE-SIGN-IN.md. This release cannot complete real sign-in until that file is supplied and the APK is rebuilt. Authentication is optional; it does not enable Drive synchronization or upload financial records.
- Backups retain reminder preferences with reminders disabled on restore, so importing a file does not immediately trigger alerts.
- Version code 23; same package and permanent signing certificate. Install as an update. Payroll calculations remain unchanged.

Tests cover all/selected reminder scope, hours/days conversion, later-added shifts, edit/completion/deletion, persistence, English/German light/dark layouts, and account UI states. Native emulator tests cover real background alarm delivery, cancellation, duplicate suppression and the unconfigured sign-in path. Real Google account authentication needs the app owner's Firebase configuration and device verification; no successful account login is claimed without it.

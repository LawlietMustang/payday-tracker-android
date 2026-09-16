# 2.2.3 — Layout, widget help and portable backup

- Expenses hides workplace selection/actions; income pages keep them.
- Native system/cutout/keyboard insets protect the WebView from status and navigation bars on Android 11+. Background follows light/dark mode and light mode uses dark status icons.
- Bottom navigation uses equal 24px vector icons rather than varying font glyphs.
- Widget pinning checks support and return status, catches launcher errors, and provides persistent manual-add help plus a home-screen shortcut. A launcher accepting a request does not guarantee the user added the widget.
- Settings → Backup & restore exports versioned JSON via Android's document picker to user-selected shared storage or a file provider such as Drive. Imports validate and confirm before replacing data. Files saved in shared Documents/Downloads or Drive survive uninstall; app-private data does not. Reinstallation requires selecting the saved file again.
- Portable backups contain saved app records and web preferences, not active timers, credentials, native app lock or reminder schedules. They are unencrypted; users should keep them private. No automatic backup schedule is implemented.
- Google OAuth sign-in and automatic Drive discovery/sync remain blocked on configuring a Google Cloud project, consent screen, package/signing-certificate-bound OAuth clients and Drive authorization. System file-picker Drive access uses the Drive provider's account, not an account connected to Payday Tracker. Never embed a client secret in the APK.

Version code 21, same application ID and permanent signing key. Browser tests cover backup round-trip/cancellation, invalid format rejection, expense-only selector hiding and widget fallback. Native emulator regression covers the existing Android integrations; physical Xiaomi launcher/Drive-provider behaviour still needs device testing.

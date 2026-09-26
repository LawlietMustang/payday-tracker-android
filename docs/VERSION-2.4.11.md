# WageTrack 2.4.11

- Generate versioned CSS/script references from Gradle without editing source HTML.
- Clear WebView cache once per app version while retaining saved data and permissions.
- Guard every exposed bridge operation and posted UI action; log local, redacted failures.
- Add read-only bridge contract inspection, documented callers and CI drift checks.
- Restore missing release notes for 2.4.5–2.4.8 from commit history.

Already present: automatic main-branch signed builds, LOAD_NO_CACHE, Android native
feature tests and the 2.4.10 embedded-icon fix. The old file-mask bug was confirmed
as the reason icons disappeared; cache hardening is preventive, not a revised diagnosis.

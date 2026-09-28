# WageTrack 2.5.0 — native layout restoration

Rebuilds the v2.4.10 purple/lime interface with Android Compose. The old JavaScript UI is not displayed. Legacy assets remain only for compatibility/reference; a minimal localStorage reader handles upgrades.

Restores the floating Overview / Hours / Add / Expenses / History bar, drawer, available-pay hero, live work/break timer, calendar, templates, multi-date shifts, status selection, expenses, savings goals, settings, onboarding, reminders, widget and backup controls.

Room v1 → v2 adds a document table without deleting records. Backup import runs in a transaction, preserves unknown v2.4.10 fields, and imports templates and planned-shift metadata. A pre-migration copy is retained locally.

Google sign-in still requires the repository owner's valid Google configuration. Android's file picker provides local/Drive backup access; sign-in does not silently enable cloud backup.

Validation evidence and remaining parity items are tracked in docs/NATIVE-PARITY.md. This file is not a claim of complete device verification.

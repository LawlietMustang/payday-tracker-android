# Payday Tracker 2.3.0 — first-run setup

A four-step welcome flow guides new users through workplace name, hourly wage, monthly hours target, currency, optional first completed shift, and Android notification permission. Setup drafts survive reopening. Existing installations retain their data and skip setup; Settings → Setup guide reopens it voluntarily.

The searchable currency picker supports 25 currencies, including EUR, USD, GBP and BDT. A single base currency is used throughout earnings, expenses, budgets, goals, payslip comparisons and CSV headings. This is not exchange-rate conversion. Currency is locked once monetary records exist.

Germany/EUR retains the existing simplified net estimate. Other currencies use a user-entered total deduction percentage; this is not a country-specific payroll calculator. German automatic holiday bonuses are inactive in manual mode. New installations start with zero bonus percentages and no assumed savings goal. Existing bonus settings are preserved.

The setup restore action uses the existing file import (including Drive when available as an Android file provider). It does not add Google authentication or automatic cloud sync. Notification permission is optional; upcoming-shift reminder timing is configured in Reminders.

Validation includes setup persistence, skipping, manual deductions, currency labels, upgrade preservation, responsive English/German light/dark layouts, and Android emulator startup alongside the existing native reminder/lock/widget smoke tests. Physical-device validation remains necessary.

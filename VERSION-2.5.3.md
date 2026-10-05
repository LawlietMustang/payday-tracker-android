# WageTrack 2.5.3 (44)

- Keep the purple clock face with a fixed-length hand and tap-to-select numbers.
- Restore AM/PM controls and editable hour/minute fields in both shift and batch editing.
- Accept hours 0–23 and minutes 0–59. Explicit 13–23 input selects PM; 00 selects AM. Hours 1–12 respect the selected period.
- Normalize 17:30 to 5:30 PM in the picker while storing 17:30. Invalid or empty values cannot be confirmed; Cancel keeps the original time.

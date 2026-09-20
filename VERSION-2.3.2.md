# WageTrack 2.3.2

- Save messages no longer intercept touches, including after they fade out over the bottom navigation.
- Holding a shift for 350 ms enters batch selection. Releasing the hold keeps it selected; further rows can be selected with a single tap.
- Selecting, deselecting, selecting all and leaving selection update existing rows without rebuilding the page or recalculating earnings.
- Moving a finger to scroll cancels the hold. Native vertical scrolling remains available.
- Timer updates change only changed clock text and no longer trigger a full-page translation scan each second.

Validation covers repeated expense saves followed by navigation, actual touch holds and cancellation, large-list selection, confirmed batch deletion, and idle/active timer updates. Android instrumentation also exercises a faded message followed by a navigation tap and a hold with real MotionEvents.

The application ID, permanent signing configuration, and stored-data format remain unchanged. Install as an update to the existing signed release.

# WageTrack 2.4.10

Fix invisible supplied icons in the installed Android app. The app loads its UI
from `file:///android_asset/`; external file URLs are blocked as CSS mask images.
The previous tests checked the mask URL, which existed even when no pixels painted.

All 11 shared icon masks now embed the original SVG drawing data in `design.css`.
This covers Settings, home shortcuts, info, warning, close, and appearance icons.
The original SVG assets and their metadata are preserved. Theme colors, icon
geometry and WebView security settings remain unchanged.

After editing a supplied SVG, run:

```sh
python3 scripts/embed-icon-masks.py
```

CI checks that the embedded data matches those assets. The Android device test
compares painted icons against hidden controls in both themes, requiring visible
strokes and transparent space. It records Settings and icon-gallery screenshots,
including an old external-file mask for comparison. This runs in the real offline
WebView, so an HTTP preview alone cannot pass this release check.

Version code: 39. Uses the same application ID and permanent signing key so it
can update an existing installation without clearing user data.

#!/usr/bin/env python3
"""Check release notes and bridge references; --write-doc refreshes the contract table."""
import re,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
assets=ROOT/'app/src/main/assets'
native=ROOT/'app/src/main/java/com/paydaytracker/app/bridge/AndroidBridge.kt'
methods=re.findall(r'@JavascriptInterface\s+fun (\w+)\(([^)]*)\)(?:: (\w+))?',native.read_text())
known={m[0] for m in methods};callers={name:[] for name in known}
for file in assets.rglob('*.js'):
 for name in set(re.findall(r'\bAndroid\.(\w+)',file.read_text())):
  if name not in known:raise SystemExit(f'{file.name}: unknown Android.{name}')
  callers[name].append(str(file.relative_to(ROOT)))
version=re.search(r'versionName = "([^"]+)"',(ROOT/'app/build.gradle.kts').read_text())[1]
if not any((ROOT/f).exists() for f in [f'VERSION-{version}.md',f'docs/VERSION-{version}.md']):
 raise SystemExit('Missing release notes for '+version)
intro='''# JavaScript ↔ Android bridge

Generated contract table: run `python3 scripts/check-project.py --write-doc` after changing the bridge.
CI rejects missing callers, stale documentation, and a version without release notes.

Only `bridge/AndroidBridge.kt` exposes annotated methods. The Activity owns UI actions.
Both calling-thread exceptions and posted UI-thread exceptions are guarded. Failure logs
contain only the method and exception class, never arguments, tokens, PINs or backup data.
Read failures return an error JSON object (or an empty consumed month); action failures
notify `window.nativeBridgeError(method)` with a rate-limited translated message.
Feature callbacks (`backupResult`, `csvResult`, reminder results) retain their existing meaning.
No Crashlytics dependency or remote error reporting is added.

`bridgeContract()` is read-only reflection metadata. Tests inspect it without calling
all methods with dummy arguments, which could delete data or launch system dialogs.
Google tokens and app PINs never cross this bridge. Times use local calendar dates;
elapsed timer values are milliseconds, reminder lead values are minutes. JSON payloads
are produced by the caller listed below and validated by the matching native feature.

| Method | Arguments | Return | JavaScript callers |
| --- | --- | --- | --- |
'''
rows=[]
for name,args,ret in sorted(methods):
 rows.append(f'| `{name}` | `{args or "none"}` | `{ret or "Unit"}` | '+(', '.join('`'+x+'`' for x in sorted(callers[name])) or 'Read-only instrumentation contract')+' |')
expected=intro+'\n'.join(rows)+'\n';doc=ROOT/'docs/JS-NATIVE-BRIDGE.md'
if '--write-doc' in sys.argv:doc.write_text(expected)
if not doc.exists() or doc.read_text()!=expected:raise SystemExit('Bridge documentation is stale; run with --write-doc')
print(f'PASS: {len(methods)} bridge methods, callers and release notes for {version}')

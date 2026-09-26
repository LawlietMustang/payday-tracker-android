# Organise New.md implementation status

| Checklist | Result |
| --- | --- |
| 1.1 Cache hardening | LOAD_NO_CACHE already existed. Added generated versioned URLs and cache-only invalidation per version; install-over regression test protects local data. |
| 1.2 Bridge | Guarded facade and UI callbacks; local redacted logs, translated errors, generated contract docs and safe reflection checks. No dummy calls to destructive methods. |
| 1.3 Build trigger | Already automated on main; retained. |
| 1.4 Release notes | Added 2.4.5–2.4.8 from actual commits. 2.4.9 and 2.4.10 already existed. CI now checks the current release notes. |
| 1.5 Pre-publish | Native/bridge CI coverage implemented. External owner tasks remain below. |
| 2 Cleanup | Separate PR; removed two unused colors, archived future refund SVG outside APK, retained active artwork, added resource lint. |
| 3 Organization | Frontend and native feature folders, updated build/load/test paths, compatibility entry points, developer guide and real upgrade verification. |

## Owner tasks before a Google Play publication

- Supply and publish the privacy policy URL in Play Console.
- Complete and verify production Google sign-in with the owner's Firebase project,
  release certificate and account; register a separate debug client if needed.
  Existing unconfigured sign-in reports setup status rather than pretending to sign in.
- Confirm an off-site copy of the permanent keystore/password is retained. The original
  signing setup already instructs retaining the locally generated key; its custody
  cannot be verified from repository contents.
- Capture final store-listing screenshots on the target phone/account.

These tasks require owner-side configuration or a publication decision. This update
neither publishes to Google Play nor creates authentication credentials.

The original report's diagnosis of a cache-only/manual-build problem was outdated:
the confirmed missing-icon cause was file-based CSS masks, fixed and pixel-tested in
2.4.10. That fix remains in place.

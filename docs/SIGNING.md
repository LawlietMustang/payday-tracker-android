# Permanent Android signing

Distributed APKs must use the same private signing key for every release. GitHub-hosted runners previously generated a new debug key for each run, making updates incompatible.

## One-time setup

On a trusted computer with Bash, Java (keytool), OpenSSL, and GitHub CLI installed, sign into GitHub CLI with an account that can manage this repository's Actions secrets:

```sh
gh auth login
gh repo clone LawlietMustang/payday-tracker-android
cd payday-tracker-android
bash scripts/setup-signing.sh
```

The script generates one private key outside the repository, stores it and its password in GitHub Actions secrets, and starts the build. Keep the local signing directory secure. Never commit the key or password. A rerun on the same computer reuses that key. Do not run setup concurrently on multiple computers.

Required secrets: PAYDAY_KEYSTORE_BASE64, PAYDAY_KEYSTORE_PASSWORD, PAYDAY_CERT_SHA256. The last value is the lowercase SHA-256 of the DER signing certificate. Builds stop if secrets are missing or the resulting APK certificate differs. The private keystore is removed from the runner after the build and is never uploaded as an artifact.

Download PaydayTracker-release-apk from the successful Actions run. Every later release must retain applicationId com.paydaytracker.app, this key, and an increasing versionCode. Debug builds use com.paydaytracker.app.debug to avoid conflicting with the installed release.

The first permanently signed APK cannot update the old randomly signed APKs. A one-time reinstall is needed if the original signing key is unavailable; uninstalling deletes app-local records. This change does not implement backup or migration.

Before declaring update compatibility verified, complete setup and build twice on separate runners, compare the certificate fingerprints, and test an update on an Android device. Missing-secret failure alone is not a successful release build.

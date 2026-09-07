#!/usr/bin/env bash
# Run once on a trusted computer; reruns reuse the original key.
set -euo pipefail
set +x
umask 077
repo=LawlietMustang/payday-tracker-android
for tool in gh keytool openssl base64; do command -v "$tool" >/dev/null || { echo "Install $tool first."; exit 1; }; done
gh auth status >/dev/null
signing_dir="${PAYDAY_SIGNING_DIR:-$HOME/.payday-tracker-signing}"
mkdir -p "$signing_dir"
chmod 700 "$signing_dir"
existing=$(gh secret list --repo "$repo" --json name --jq '.[].name')
if [[ ! -f "$signing_dir/release.p12" ]]; then
  if [[ "$existing" == *PAYDAY_KEYSTORE* || "$existing" == *PAYDAY_CERT_SHA256* ]]; then
    echo 'Signing secrets already exist. Restore the original signing directory; do not generate another key.'; exit 1
  fi
  if [[ -e "$signing_dir/password" ]]; then echo 'Incomplete local setup. Check the signing directory before proceeding.'; exit 1; fi
  openssl rand -hex 32 > "$signing_dir/password"
  keytool -genkeypair -keystore "$signing_dir/release.p12" -storetype PKCS12 \
    -alias payday -keyalg RSA -keysize 3072 -validity 10000 \
    -storepass:file "$signing_dir/password" -dname 'CN=Payday Tracker' >/dev/null 2>&1
fi
keytool -exportcert -keystore "$signing_dir/release.p12" -alias payday \
  -storepass:file "$signing_dir/password" -file "$signing_dir/certificate.der" >/dev/null 2>&1
openssl dgst -sha256 "$signing_dir/certificate.der" | awk '{print $NF}' > "$signing_dir/fingerprint"
# Mark setup first so an interrupted upload cannot silently replace the key.
gh secret set PAYDAY_CERT_SHA256 --repo "$repo" < "$signing_dir/fingerprint"
gh secret set PAYDAY_KEYSTORE_PASSWORD --repo "$repo" < "$signing_dir/password"
base64 < "$signing_dir/release.p12" | gh secret set PAYDAY_KEYSTORE_BASE64 --repo "$repo"
echo "Signing is configured. Keep $signing_dir private and securely retain it for future releases."
gh workflow run build-apk.yml --repo "$repo" --ref main

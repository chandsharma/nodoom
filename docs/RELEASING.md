# Release process

Release signing material belongs to the repository owner and must never be committed, uploaded to GitHub Actions or shared in an issue.

## One-time owner key

Create a dedicated Android signing key in the ignored `keystore/` directory. Store the keystore and its password in the owner's password manager plus an offline backup. Losing it prevents future in-place updates.

Create an ignored `signing.properties` file:

```properties
storeFile=keystore/nodoom-release.jks
storePassword=OWNER_SECRET
keyAlias=nodoom
keyPassword=OWNER_SECRET
```

## Build and verify

```bash
./gradlew clean test lint assembleRelease
$ANDROID_HOME/build-tools/34.0.0/apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
shasum -a 256 app/build/outputs/apk/release/app-release.apk
```

Before publishing:

- Complete the relevant physical-device checklist.
- Confirm the merged manifest has no Internet, storage, location, camera, microphone, contacts, phone or usage-access permission.
- Record app version, commit, Android version, Pixel model and Instagram version.
- Install the APK over the previous release without clearing state.
- Verify the signing-certificate SHA-256 matches the previous release.

Attach the APK and `SHA256SUMS.txt` to a GitHub Release. Include one-line changes, test status and known limitations. Alpha artifacts must be clearly marked pre-release.


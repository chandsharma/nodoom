# Release process

Release signing material belongs to the repository owner and must never be committed, uploaded to GitHub Actions or shared in an issue.

## One-time owner key

Create a dedicated Android signing key in the ignored `keystore/` directory. Store the keystore and its password in the owner's password manager plus an offline backup. Losing it prevents future in-place updates.

For the owner's Mac, the alpha signing key is stored at `keystore/nodoom-release.jks` and its generated password is stored in macOS Keychain under service **NoDoom Android signing key**, account **chandsharma**. The key and password are not in Git or GitHub.

Retrieve the password for backup without placing it in shell history (the command prints it to the terminal, so do this privately):

```bash
security find-generic-password -a chandsharma -s "NoDoom Android signing key" -w
```

On another machine, create an ignored `signing.properties` file:

```properties
storeFile=keystore/nodoom-release.jks
storePassword=OWNER_SECRET
keyAlias=nodoom
keyPassword=OWNER_SECRET
```

## Build and verify

```bash
./scripts/build-owner-release.sh
$ANDROID_HOME/build-tools/36.0.0/apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
shasum -a 256 app/build/outputs/apk/release/app-release.apk
```

Before publishing:

- Complete the relevant physical-device checklist.
- Confirm the merged manifest has no Internet, storage, location, camera, microphone, contacts, phone or usage-access permission.
- Record app version, commit, Android version, Pixel model and Instagram version.
- Install the APK over the previous release without clearing state.
- Verify the signing-certificate SHA-256 matches the previous release.

Attach the APK and `SHA256SUMS.txt` to a GitHub Release. For Google Play, upload the signed `app/build/outputs/bundle/release/app-release.aab`; the same owner key is the upload key while Play App Signing protects store distribution. Include changes, test status and known limitations in every release.


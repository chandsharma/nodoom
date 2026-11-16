# Install No Doom on a Pixel

No Doom is not on Google Play. Install only an APK from this repository's Releases page.

## Before installing

1. Confirm that Instagram was installed from Google Play.
2. Download the release APK and `SHA256SUMS.txt` from the same GitHub release.
3. Verify the APK checksum on a computer when possible.
4. Do not install an APK from an issue, mirror, direct message or third-party download site.

## Install

1. Open the downloaded APK on the Pixel.
2. Android may ask to allow the browser or Files app to install unknown apps. Allow it for this installation, install No Doom, then turn that allowance back off.
3. If Play Protect asks to scan, let it scan. Do not disable Play Protect globally.
4. Open **No Doom** and read the welcome screen.

### If Play Protect blocks the installation

In selected markets, Play Protect automatically blocks apps installed from a browser or file manager when they declare an Accessibility service. Signing and a clean malware scan do not bypass that distribution rule. A Play Protect appeal for the owner-signed package was submitted on 16 November 2026.

Do not turn off Play Protect. For owner/developer testing, Android Debug Bridge is the supported temporary path:

```bash
adb install -r NoDoom.apk
```

The public GitHub APK remains the authoritative artifact; ADB changes only the installation route.

## Enable the required controls

No Doom walks through each item and displays its live status.

### Accessibility

For a sideloaded app, Pixel may initially block the Accessibility switch:

1. Open **Settings → Apps → No Doom**.
2. Tap the three-dot menu at the top right.
3. Tap **Allow restricted settings** and authenticate.
4. Return to **Settings → Accessibility → Downloaded apps → No Doom**.
5. Turn on **Use No Doom** and accept Android's disclosure.

Accessibility is powerful. The open-source app has no Internet permission. It receives foreground package transitions so it can remove its gate when you leave Instagram, but inspects UI structure only inside Instagram, Android System UI and the two removal-barrier system screens. Other apps' window content is not requested or traversed.

### Notifications

Allow notifications so the phone can announce the 10-minute unlock window. There is no permanent “running” or “protecting you” notification by default. **No Doom → Settings** controls the unlock alert, optional always-on status, optional silent countdown and optional Instagram-change warning. A separate blocked-swipe toast is also off by default and can be enabled there.

### Exact timer

Allow **Alarms & reminders**. This schedules exactly two local events—the start and end of an unlock window—and does not keep the app running or awake.

### Uninstall barrier

Activate No Doom as a Device Administrator. No Doom requests no device-admin policies and cannot wipe, lock or change the phone password. To remove it later, open **No Doom → Settings → Remove app**, complete the 10-minute wait, then use Android's normal uninstall confirmation.

## Live check

1. In No Doom, tap **Open Instagram for live check**.
2. Open a Reel from Home, a profile or Explore. Try to swipe vertically five times. The starting Reel must remain.
3. Leave the Reel and verify that Home/Explore/Profile still scroll normally.
4. Tap Instagram's Reels tab. The first Reel may play, but five vertical swipes must leave that same Reel visible.
5. While that Reel is open, pull down the Android notification shade and close it again. Both gestures must work normally.
6. Return to Home and immediately scroll several posts in both directions. No gesture may be blocked.
7. Return to No Doom. Both recognition rows must show timestamps.

If either check fails, do not treat that build as protection. Record the Pixel's Android version and Instagram version in a compatibility issue without including private screen content.

## Updating

Install a newer signed APK directly over the existing version. Never uninstall first: uninstalling removes state. Android will accept an in-place update only when it is signed by the same owner-controlled key.


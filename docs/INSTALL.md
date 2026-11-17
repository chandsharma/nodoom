# Install No Doom on a Pixel

No Doom is being submitted to Google Play. Until its listing is approved, install only an APK from this repository's Releases page.

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

Accessibility is powerful. Before opening Android Settings, No Doom shows a prominent disclosure and asks you to affirmatively agree. The open-source app has no Internet permission. It receives foreground package transitions so it can remove its gate when you leave Instagram, but inspects UI structure only inside Instagram and Android System UI. Other apps' window content is not requested or traversed.

### Optional notifications

Protection works without notification permission. In **No Doom → Settings**, you can optionally enable the one alert for the 10-minute decision window, a silent countdown status, an Instagram-version warning or a rate-limited blocked-swipe hint. Routine protection remains quiet.

### Payments and uninstall

Some banking and payment apps reject every downloaded Accessibility service, even while No Doom is suspended outside Instagram. Use **No Doom → Settings → Turn off Accessibility for payments**, then restart the phone if the payment app requests it. Android does not allow No Doom to re-enable itself; turn it on manually afterward.

No Doom uses Android's standard uninstall flow and never registers as Device Administrator.

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


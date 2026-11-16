# No Doom

**Use the official Instagram app—without falling into the infinite Reels feed.**

No Doom is an open-source Android companion app that lets you watch the Reel you intentionally opened, then blocks the swipe to the next Reel. It also redirects Instagram's Reels tab back to Home.

## Download for Android

### [⬇️ Download No Doom APK](https://github.com/chandsharma/nodoom/releases/latest/download/NoDoom.apk)

**Current version:** `0.1.0-alpha.1` · **Requires:** Android 14 or newer

[View release notes and checksums](https://github.com/chandsharma/nodoom/releases/latest) · [Read the installation guide](docs/INSTALL.md)

This is an owner-signed alpha release for Pixel 9 Pro feasibility testing. Complete the in-app live check after installation before relying on its protection.

- No root, bootloader unlock, patched APK, clone, or unofficial Instagram client.
- Works alongside the official Instagram app installed from Google Play.
- No Instagram login and no Instagram or Meta API access.
- No Internet permission, ads, analytics, telemetry, or cloud account.
- A 60-minute cooling-off period plus a 10-minute confirmation window makes impulsive disabling harder.

> [!IMPORTANT]
> No Doom is a real daily-use app under active development, not a tutorial project. The current `0.1.0-alpha.1` code is awaiting the required M0 feasibility pass on the owner's Pixel 9 Pro and current Instagram. Do not rely on an alpha build for protection until the device test report is published.

## What it changes

| Place | Behaviour |
|---|---|
| Home, Explore, profiles, DMs, stories, comments | Unchanged; scrolling works normally |
| A Reel you intentionally open | Plays normally; controls remain available |
| Swipe to another Reel | Blocked |
| Instagram Reels tab | Covered and redirected to Home |
| Other apps | Never touched |

No Doom uses Android Accessibility to recognise a full-screen Reel and intercept only the vertical gesture that would advance it. The recognition rules live in one file, [`InstagramUiClassifier.java`](app/src/main/java/com/chandsharma/nodoom/InstagramUiClassifier.java), so Instagram compatibility can be repaired quickly after UI changes.

## Safety and privacy

Instagram remains the Play Store-signed app and handles every login, video, message, like, comment, and network request. No Doom does not contact Instagram and does not perform social actions for you.

The release manifest intentionally has **no `INTERNET` permission**. Screen structure is inspected in memory only for Instagram and the small set of Android system screens needed for self-protection. Release builds do not store captions, messages, usernames, images, or accessibility-tree dumps. See [Privacy](docs/PRIVACY.md) and [Architecture](docs/ARCHITECTURE.md).

## Install

There is no Play Store listing. Owner-signed APKs are published on this repository's [GitHub Releases](https://github.com/chandsharma/nodoom/releases), together with SHA-256 checksums. Follow the [plain-language installation guide](docs/INSTALL.md); Android requires an explicit “Allow restricted settings” step before a sideloaded accessibility service can be enabled.

Never install an APK posted in an issue, discussion, comment, mirror, or chat. Use only this repository's Releases page and verify its checksum.

## Build from source

Requirements:

- JDK 17
- Android SDK Platform 34 and Build Tools 34.0.0
- Android Studio or the command-line SDK tools

```bash
./gradlew test assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/`. Release signing is deliberately local and the owner's key is never committed. The owner can build the signed artifact with `./scripts/build-owner-release.sh`; see [Releasing](docs/RELEASING.md).

## Current milestone

The project follows five gates:

1. **M0 feasibility:** validate every entry point and compare gesture suppression with bounce-back on the Pixel 9 Pro.
2. **M1 Reel blocking:** pass all Reel and unaffected-screen tests.
3. **M2 lock:** validate the 60-minute countdown and 10-minute window.
4. **M3 self-protection and setup:** validate Settings, uninstall and reboot paths.
5. **M4 hardening:** battery, memory, privacy, upgrade and one-week daily-use test.

Progress and evidence are tracked in [the feasibility report](docs/FEASIBILITY.md) and [test report](docs/TEST_REPORT.md). A checkbox is not marked passed without a real-device observation.

## Contributing

Contributions are welcome—especially accessibility snapshots that omit personal text, translations, Pixel compatibility fixes, UI tests and careful reviews of false-positive risks. Please read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a pull request.

Useful labels and GitHub topics: `android`, `digital-wellbeing`, `doomscrolling`, `instagram`, `accessibility-service`, `open-source`, `productivity`, `pixel`.

## Licence

Apache-2.0. Instagram is a trademark of Meta Platforms, Inc. No Doom is an independent project and is not affiliated with or endorsed by Instagram or Meta.


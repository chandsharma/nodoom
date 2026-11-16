# Acceptance test report

Status: **0.1 and 0.2 exposed physical-device failures; 0.3.0-alpha.3 awaits retest.**

| Field | Value |
|---|---|
| Device | Pixel 9 Pro (`caiman`) |
| Android version/build | Pending capture |
| Instagram version | 454.0.0.54.77 |
| No Doom version | 0.1 and 0.2 tested; 0.3.0-alpha.3 pending |
| Test date | 16 Nov 2026 |

The authoritative acceptance checklist is in `PRODUCT_REQUIREMENTS.md`. Results must be recorded as **Pass**, **Fail** or **Not run** with evidence; empty checkboxes must never be interpreted as a pass.

## Automated checks

| Check | Result |
|---|---|
| Unit tests | Pass — includes inline-feed/full-screen and System UI isolation regressions, 16 Nov 2026 |
| Android lint | Pass, zero issues — 16 Nov 2026 |
| Debug APK | Pass — assembled successfully from the release source |
| Release APK signature | Pass — owner key, RSA 4096, certificate SHA-256 `5822e80c3db9b629f0cd4e8a35fd274da98892a54ee60d5d0cb8fd653f1db28e` |
| Manifest permission audit | Pass — only notifications, boot recovery and exact alarms; no Internet permission |

## Physical findings from 0.1.0-alpha.1

| Area | Result |
|---|---|
| Home feed | **Fail** — inline Reel geometry intermittently activated the full-screen gesture gate and stopped ordinary feed scrolling |
| Reels tab | **Fail** — selected-state recognition was delayed; several swipes could occur before redirect |
| Full-screen entry points | **Fail** — blocking was inconsistent across Home and profile entry points |
| App/Settings UX | **Fail** — permissions and live-test evidence were mixed together; broad self-protection matching caused false exits |
| Noise | **Fail** — repeated block toasts and a self-protection notification felt like spam |
| Icon | **Fail** — placeholder launcher mark was not production quality |

Version 0.2.0-alpha.2 addressed the first observations, but the follow-up device pass below exposed additional gesture-window failures.

## Additional physical findings from 0.2.0-alpha.2

| Area | Result |
|---|---|
| Notification shade | **Fail** — the gesture overlay could remain above System UI and prevent the shade from closing |
| Home feed | **Fail** — a transient/incomplete accessibility tree could still gate ordinary scrolling |
| Reels tab | **Fail** — protection could drop after transient tree changes and allow multiple Reels |
| Noise | **Fail** — the installed build still exposed unwanted feedback during the affected flows |

Version 0.3.0-alpha.3 excludes the status-bar strip, listens for System UI foreground windows, verifies the foreground package before restoring the gate, immediately releases ordinary navigation surfaces, confirms unselected viewers twice, and makes all routine feedback opt-in. No physical result is marked passed until the owner retests the signed artifact.

## Physical acceptance

T-01 through T-54: **Not passed**. The full matrix plus notification-shade and immediate-Home-scroll regressions must be rerun on 0.3.

## Performance measurements

| Measurement | Result |
|---|---|
| APK size | 0.3 signed/minified release: 204,052 bytes; SHA-256 `0453f6343b2e4d8d6faf5e930212246b0791eeb615c5f22e751ffdd220f59bef` |
| Idle memory | Pending |
| Two-hour Instagram battery impact | Pending |
| Overnight wake-ups | Pending |

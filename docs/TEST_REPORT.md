# Acceptance test report

Status: **0.1.0-alpha.1 failed its first physical-device pass; 0.2.0-alpha.2 awaits retest.**

| Field | Value |
|---|---|
| Device | Pixel 9 Pro (`caiman`) |
| Android version/build | Pending capture |
| Instagram version | 454.0.0.54.77 |
| No Doom version | 0.1.0-alpha.1 tested; 0.2.0-alpha.2 pending |
| Test date | 16 Nov 2026 |

The authoritative acceptance checklist is in `PRODUCT_REQUIREMENTS.md`. Results must be recorded as **Pass**, **Fail** or **Not run** with evidence; empty checkboxes must never be interpreted as a pass.

## Automated checks

| Check | Result |
|---|---|
| Unit tests | Pass — includes inline-feed/full-screen classifier regression tests, 16 Nov 2026 |
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

Version 0.2.0-alpha.2 addresses these observations but no physical result is marked passed until the owner retests the signed artifact.

## Physical acceptance

T-01 through T-54: **Not passed**. Relevant 0.1 findings are recorded above; the full matrix must be rerun on 0.2.

## Performance measurements

| Measurement | Result |
|---|---|
| APK size | Signed/minified release: 199,640 bytes; SHA-256 `14dcda61645de3b3b53124e707cf40c0a0b96d69f30adaea52ab8f2614a8835b` |
| Idle memory | Pending |
| Two-hour Instagram battery impact | Pending |
| Overnight wake-ups | Pending |


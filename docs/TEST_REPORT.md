# Acceptance test report

Status: **No physical-device tests have been claimed.**

| Field | Value |
|---|---|
| Device | Pixel 9 Pro (pending connection) |
| Android version/build | Pending |
| Instagram version | Pending |
| No Doom version | 0.1.0-alpha.1 |
| Test date | Pending |

The authoritative acceptance checklist is in `PRODUCT_REQUIREMENTS.md`. Results must be recorded as **Pass**, **Fail** or **Not run** with evidence; empty checkboxes must never be interpreted as a pass.

## Automated checks

| Check | Result |
|---|---|
| Unit tests | Pass — 16 Nov 2026 |
| Android lint | Pass, zero issues — 16 Nov 2026 |
| Debug APK | Pass — 80,589 bytes, `6ff0cbb8e4cbf9dab8331e5723b39a5b112fdc1a396ae44e1ae98121ae70f82f` |
| Release APK signature | Pass — owner key, RSA 4096, certificate SHA-256 `5822e80c3db9b629f0cd4e8a35fd274da98892a54ee60d5d0cb8fd653f1db28e` |
| Manifest permission audit | Pass — only notifications, boot recovery and exact alarms; no Internet permission |

## Physical acceptance

T-01 through T-54: **Not run**.

## Performance measurements

| Measurement | Result |
|---|---|
| APK size | Signed/minified release: 54,996 bytes; SHA-256 `639fa81596a033df92c51cccbe6554b8673fa79344b9c36a918c38264745b7fe` |
| Idle memory | Pending |
| Two-hour Instagram battery impact | Pending |
| Overnight wake-ups | Pending |


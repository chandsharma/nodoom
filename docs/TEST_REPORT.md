# Acceptance test report

Status: **0.5.0-alpha.6 passed the focused Home-isolation and one-Reel regression; the full acceptance matrix and extended-use checks remain open.**

| Field | Value |
|---|---|
| Device | Pixel 9 Pro (`caiman`) |
| Android version/build | Android 17 / API 37; `google/caiman/caiman:17/CP3A.261005.005/16271449:user/release-keys` |
| Instagram version | 454.0.0.54.77 |
| No Doom version | 0.1 through 0.5.0-alpha.6 tested |
| Test date | 16 Nov 2026 |

The authoritative acceptance checklist is in `PRODUCT_REQUIREMENTS.md`. Results must be recorded as **Pass**, **Fail** or **Not run** with evidence; empty checkboxes must never be interpreted as a pass.

## Automated checks

| Check | Result |
|---|---|
| Unit tests | **Pass — 38 tests in each of the debug and release variants, zero failures/errors/skips**, including exact navigation, inline-feed/full-screen, modal-transition, sheet-recognition and System UI isolation regressions, 16 Nov 2026 |
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

## Physical findings from 0.3.0-alpha.3

| Area | Result |
|---|---|
| Upgrade/install | **Pass** — upgraded in place from 0.1 with the owner certificate; data, notification permission and device-admin state remained intact |
| App launch | **Pass** — cold launch completed without an AndroidRuntime or window error |
| Quiet default | **Pass** — no active No Doom notification; legacy high-priority channel deleted |
| Home feed | **Fail** — lower-feed clickable controls produced too many bottom candidates, hiding the true navigation-row signal and activating the Reel gate on Home |
| Leaving Instagram | **Fail** — manifest package filtering prevented events from unrelated foreground apps, so a gate could remain after switching to No Doom itself |

Version 0.4.0-alpha.4 narrows navigation candidates to the Pixel's lowest Instagram band and receives global package transitions while refusing to inspect unrelated app trees.

## Focused physical findings from 0.4.0-alpha.4

| Area | Result |
|---|---|
| Upgrade/install | **Pass** — owner-signed APK upgraded in place; Accessibility remained enabled and bound |
| Home feed | **Pass** — immediate and repeated vertical scrolling worked with no gesture gate |
| Reels tab | **Pass** — first Reel remained visible and the gate stayed active through five vertical advance attempts |
| Native navigation | **Pass** — the overlay ended at Instagram's detected navigation boundary; Home opened natively and remained scrollable |
| Reel controls | **Pass** — comments and sharing opened through native accessibility actions; their sheets scrolled without the gate and protection restored after closing |
| Notification shade | **Pass** — opened and closed from a protected Reel; the gate was absent over System UI and restored immediately on return |
| Leaving Instagram | **Pass** — opening No Doom removed the gate; returning to the protected Reel restored it |
| Quiet default | **Pass** — no repeated toast, active protection notification or release diagnostic logging |
| Privacy/signing | **Pass** — no Internet permission; one owner signer; certificate SHA-256 `5822e80c3db9b629f0cd4e8a35fd274da98892a54ee60d5d0cb8fd653f1db28e` |

The 0.4 control hand-off still had a race: a comments/share/save transition could leave the gate absent long enough for one or two short next-Reel swipes, and generic Accessibility clicks did not reliably preserve every native Reel control.

## Focused physical findings from 0.5.0-alpha.5

| Area | Result |
|---|---|
| Upgrade/install | **Pass** — owner-signed candidate upgraded in place and the Accessibility service rebound |
| Reels tab | **Pass** — the same Reel remained visible through five mixed short/long vertical advance attempts and the gate remained active |
| Comments | **Pass** — sheet opened and its exact list accepted forward scroll actions; after closing, two short advance swipes did not leave the chosen Reel |
| Sharing | **Pass** — the first share-sheet swipe scrolled its exact recipient list; immediate short swipes after closing remained blocked |
| Like and save | **Pass** — like/unlike and save/unsave changed Instagram state correctly; the saved-collection list scrolled, its close transition remained protected, and each test state was restored |
| Edge hold | **Pass with alpha timing note** — Instagram entered its 2× held-playback state; protection independently rearmed after the replay completed |
| Quiet operation | **Pass** — no release diagnostic logging; notification and blocked-swipe feedback remain opt-in |
| Previous regression set | **Pass** — Home scrolling, native bottom navigation and notification-shade open/close with immediate gate restoration remain covered |

The follow-up daily-use check exposed that Instagram reuses `sticky_header_list` for its normal Home feed and sometimes stores the selected Home state only on a nested tab icon. Full-height inline Reels also placed three feed controls in the same geometry band as the five navigation tabs. Together, those signals could leave the Reel gate above Home.

## Focused physical findings from 0.5.0-alpha.6

| Area | Result |
|---|---|
| Upgrade/install | **Pass** — the owner-signed APK upgraded in place over alpha.5; Accessibility remained enabled and was rebound to the new code |
| Sustained Home feed | **Pass** — 15 consecutive full-height swipes crossed ordinary posts and full-height inline Reels with the Reel gate absent throughout |
| Reels tab | **Pass** — the same Reel, account and engagement counts remained visible through three vertical advance attempts; the gate remained attached |
| Reels to Home | **Pass** — selecting Home removed the gate by the first 200 ms observation; five subsequent Home swipes remained native |
| Close and reopen | **Pass** — force-closing Instagram removed any gate; reopening on Home showed no gate and five subsequent feed swipes remained native |
| Classifier regression | **Pass** — exact bottom-tab IDs, descendant selected state and reused Home list IDs are covered by unit rules |
| Stability/noise | **Pass** — no No Doom crash, ANR, notification or blocked-swipe toast was observed; release diagnostic logging is absent |
| Privacy/signing | **Pass** — no Internet permission; one owner signer; certificate SHA-256 `5822e80c3db9b629f0cd4e8a35fd274da98892a54ee60d5d0cb8fd653f1db28e` |

## Physical acceptance

The focused regressions above pass. T-01 through T-54 are **not collectively passed**: DM, story, Explore, profile, search/audio/hashtag, saved, notification and external-link entry points plus long-press/multi-touch and timer/removal flows still require a complete owner run.

## Performance measurements

| Measurement | Result |
|---|---|
| APK size | 213,564 bytes (`NoDoom-0.5.0-alpha.6.apk`) |
| APK SHA-256 | `9e23d4507f23a404ce2cb0e2168e2dddfcb2dea3175a03e19b908afe986bf650` |
| Idle memory | Pending |
| Two-hour Instagram battery impact | Pending |
| Overnight wake-ups | Pending |

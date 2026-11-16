# M0 feasibility report

Status: **0.1 and 0.2 failed focused Pixel validation; 0.3 awaits retest**

An APK compiling successfully is not proof that another app's current accessibility tree and touch behaviour meet the product requirements. M0 remains a hard gate.

## Environment

| Item | Value |
|---|---|
| Date | 16 Nov 2026 initial test |
| Device | Pixel 9 Pro |
| Android version/build | Not recorded |
| Navigation mode | Not recorded |
| Instagram version | 454.0.0.54.77 |
| No Doom version | 0.1 and 0.2 tested; 0.3.0-alpha.3 pending |

## Candidate A: suppress the swipe

Implementation: a transparent Accessibility overlay consumes a clearly vertical gesture and replays non-vertical gestures.

Expected advantage: the next Reel is never requested and the starting Reel keeps playing.

Risk to validate: tap-and-hold is replayed after gesture classification, which may not preserve Instagram's exact pause timing. Multi-touch and UI controls must also pass R-03.

Results on 0.1 and 0.2: **Fail**. Inline Home Reels or an incomplete Home tree could activate the gate; the gate could remain in front of the Android notification shade; and genuine Reels-tab protection could drop after transient tree changes. Version 0.3 isolates System UI, leaves the status-bar strip native, confirms an unselected viewer twice, releases ordinary navigation immediately and keeps a click-established Reels session only while the viewer signature remains. Screen recording: Not captured.

## Candidate B: bounce back

Implementation: not enabled. It will be implemented only if Candidate A cannot pass R-03, because it can expose or restart media and is lower in the owner's preference order.

Result: Not tested. Screen recording: Not captured.

## Entry-point matrix

Home and profile entry points were sampled but failed consistency requirements. All entry points must be rerun on 0.3: Home, one-to-one DM, group DM, story share, Explore, own/other profile, search, audio, hashtag, saved collection, notification, external link and a Reel opened from another Reel context. Pulling down and closing the notification shade from a protected Reel is now an explicit regression test.

## Go/no-go decision

Pending owner review of device evidence. No production release should be labelled stable before this report names the selected behaviour and records R-03 compatibility.


# 

## 1. Overview

No Doom is a small Android app for the owner's Pixel 9 Pro. It lets the owner open and watch any single reel in the official Instagram app, but never scroll from one reel to the next. The rest of Instagram (feed, DMs, stories, Explore, profiles) works exactly as before.

**Problem.** Instagram's full-screen reel feed is built for endless swiping, and the owner loses hours to it without noticing. Instagram's own time limits can be dismissed with one tap.

**Goals**

- G1. Any reel the owner taps can be watched. A second reel reached by swiping never can.
- G2. The Reels tab is unusable: opening it lands on Home instead.
- G3. Switching No Doom off always takes at least 60 minutes, long enough for an impulse to pass.
- G4. No extra risk to the owner's Instagram account: Instagram stays the unmodified Play Store app.
- G5. No noticeable battery drain, lag or glitches.

**Success looks like**

- In a normal week of use, the owner never sees a second reel in a row in the full-screen viewer.
- Instagram feels identical outside the reel viewer.
- Without a computer or safe mode, there is no way to switch No Doom off in under 60 minutes.

Requirement IDs (R-, L-, S-, N-, I-, T-) are used throughout so tests and questions can point to them. "Must" marks a hard requirement; "should" marks a target the developer may trade off only with the owner's approval. This document says what No Doom must do, not how to build it; technical choices are the developer's, within the constraints in section 2.

## 2. Decisions and hard constraints

The owner has already made these decisions, and the developer must not change them. If one proves impossible, raise it with the owner before changing course.

| # | Decision |
| --- | --- |
| D1 | Instagram stays the official app from the Play Store. No modified, repackaged, cloned or patched Instagram. No rooting, no Xposed/LSPosed-style hooking, no bootloader unlocking. |
| D2 | No Doom is a separate app that works alongside Instagram, using Android's Accessibility permission. |
| D3 | No Doom never contacts Instagram or Meta servers, never uses Instagram's APIs, and never performs an Instagram action (like, follow, comment, view, message) for the owner. Inside Instagram it may only do what is needed to enforce section 4. |
| D4 | Reels tab: blocked completely. |
| D5 | Swiping to the next reel: the preferred result is that the swipe simply does nothing and the current reel keeps playing untouched. Only if that cannot be made glitch-free, the fallback is an instant bounce back to the same reel (R-10 to R-12). |
| D6 | Turning off: a 60-minute countdown, then a 10-minute window to confirm. If the window is missed, the next request starts again from 60 minutes. a notification should be popped that timer is done and you may turn it off. |
| D7 | Once turned off, No Doom stays off until the owner turns it back on. Turning on is instant. |
| D8 | While on, No Doom stays on through reboots, app updates and the system stopping apps in the background, with no action from the owner. |
| D9 | Only the permissions it truly needs, minimal battery and memory use, no internet access. |
| D10 | Personal use on one phone. Not published on the Play Store. |
| D11 | The app is named **No Doom**. |

## 3. Definitions

| Term | Meaning |
| --- | --- |
| Reel | A short vertical video post on Instagram. |
| Reel viewer | Any Instagram screen where one reel fills the screen and swiping up or down moves to a different reel. This includes such screens Instagram adds in future versions, not only today's. |
| Reels tab | The Reels button in Instagram's bottom bar and the endless reel feed it opens. |
| Entry point | Any place a reel can be opened from: home feed, DM, story, Explore, a profile, a search, audio or hashtag page, a notification, a link from another app. |
| Starting reel | The reel the owner tapped (or the link opened) that brought up the reel viewer. |
| Advance | The reel viewer showing any reel other than the starting reel, whatever the cause: swipe up, swipe down, Instagram's auto-scroll, end-of-video auto-advance or anything else. |
| Protection | No Doom enforcing sections 4 and 6. |
| Protected states | ON, COUNTDOWN and UNLOCK WINDOW (section 5). Protection is fully active in all three. |
| Owner | The person whose phone this is and who uses No Doom. |

## 4. Reel blocking rules

The rule is one reel per reel viewer. The owner can open any reel from anywhere and watch it in full, but the viewer never moves on to another reel. Nothing outside the reel viewer and the Reels tab changes.

### 4.1 The one-reel rule

- R-01. Whenever a reel viewer opens, by any entry point, the starting reel plays normally.
- R-02. While protection is on, the reel viewer must never advance, whatever the cause (see "Advance" in section 3).
- R-03. Everything the owner can do with the starting reel keeps working exactly as in Instagram: play, pause (tap or hold), looping, sound on/off, double-tap like, like button, comments (open, scroll, type, post, reply), share/send, save, remix, the "more" menu, expanding the caption, tapping the creator, audio, hashtags or tagged people, follow, and leaving.
- R-04. Leaving the viewer (back gesture, Back button, Instagram's close/back arrow) always works and returns the owner to where they came from.
- R-05. Each new reel viewer has its own starting reel. If the owner leaves a viewer and taps another reel, that reel opens and plays. Chaining reels by tapping is allowed by design; only swiping through is blocked.

### 4.2 Entry points

| Entry point | Expected behaviour |
| --- | --- |
| Reel in the home feed | Plays inline as normal. Tapping it opens the viewer with that reel only. |
| Reel sent in a DM (one-to-one or group) | Tapping it opens that reel only. |
| Reel shared in a story | The story plays as normal and swiping between stories still works. Opening the reel from the story gives that reel only. |
| Explore | The grid scrolls normally. Tapping a reel gives that reel only. Tapping a photo or carousel opens the post list, which scrolls normally. |
| Reels on a profile (own or others') | The grid scrolls normally. Tapping a reel gives that reel only. |
| Search results, audio, hashtag and location pages, saved posts, collections | Grids and lists scroll normally. Tapping a reel gives that reel only. |
| Instagram notification about a reel | Opens that reel only. |
| Link from another app or the browser (for example an instagram.com/reel/ link in WhatsApp) | Instagram opens that reel only. |
| Reel opened from inside a reel (creator's profile, audio page, remix) | Allowed, as in R-05. Each new viewer is one reel. |
| Reels tab | Blocked (4.3). |

### 4.3 Reels tab

- R-06. While protection is on, whenever the Reels tab would be shown, the owner must land on Instagram's Home tab instead. This covers tapping the Reels button, Instagram opening on the Reels tab, a shortcut or deep link to it, and returning to Instagram after it was left on the Reels tab.
- R-07. No reel from the Reels tab may ever be heard. Target: nothing from the Reels tab visible at all. Maximum: about 0.2 seconds, and anything visible in that time must be covered by a No Doom screen.
- R-08. A short, non-blocking message says the Reels tab is off (section 7.4). Repeated taps must not stack or spam messages.
- R-09. If Instagram moves, renames or redesigns the Reels button, or adds a new route into an endless reel feed (for example a full-screen home feed or a reels row that opens an endless viewer), R-02 and R-06 still apply.

### 4.4 What a blocked swipe looks like (owner's order of preference)

- R-10 (preferred). The swipe does nothing. The starting reel keeps playing with no stutter, restart, jump or change in sound. No part of the next reel is seen or heard.
- R-11 (fallback). Used only if R-10 cannot meet R-03 in full. The advance is undone at once and the owner lands back on the starting reel. The next reel must never be heard, and anything of it that shows must be covered by a No Doom screen within about 0.15 seconds. If the starting reel restarts from the beginning, the feasibility report must say so.
- R-12. The developer tries both in milestone M0, shows the owner screen recordings of each, and records which one is used. Whichever is chosen must meet R-03 in full. Last resort for any single screen where neither works reliably: close the viewer and return the owner to where they came from. Every such screen must be listed in the feasibility report and approved by the owner. (make sure we do not do endless and necessary testing)
- R-13. A blocked swipe may show a short, subtle "Save your brain from Doom" type hint (section 7.4), at most once every few seconds. No sound.

### 4.5 What must stay untouched

- R-14. Scrolling and everything else behaves exactly as normal in: the home feed (including inline reels and suggested-reels rows), DMs and chat lists, the stories viewer, the Explore grid and post lists, profiles, search, comments, activity/notifications, Instagram settings, camera, posting and editing.
- R-15. No Doom adds no overlays, banners, delays or lag anywhere in Instagram apart from the messages in section 7.4.
- R-16. No Doom acts only inside Instagram. Other apps (WhatsApp, Chrome, YouTube and so on) are never affected, apart from the Settings and uninstall screens covered in section 6.

### 4.6 Timing and coverage

- R-17. Protection is in force from the moment Instagram appears on screen: cold start, from recent apps, from a notification, from a link, after unlocking the phone, in split screen. There is no warm-up period during which swiping works.
- R-18. If Instagram is already on screen when protection starts (No Doom turned on, or No Doom ready after a reboot), the rules apply immediately: an open reel viewer treats its current reel as the starting reel, and an open Reels tab is sent to Home.
- R-19. Works whatever Instagram's language, the phone's font and display size, light or dark theme, and gesture or 3-button navigation.
- R-20. Works across all Instagram accounts signed in on the phone, including after switching accounts.

## 5. Turning No Doom off and on

Turning No Doom off always costs a 60-minute wait followed by a 10-minute window to confirm; turning it on is instant. Protection stays fully active until the very moment the owner confirms inside the window.

&#91;embedded content: No Doom states · 3 protected, 1 off\]

Cancelling, missing the window or rebooting always lands back on ON; only confirming inside the unlock window reaches OFF.

### 5.1 States

| State | Protection | What the owner can do |
| --- | --- | --- |
| ON | Active | Request to turn off (starts the countdown). |
| COUNTDOWN (60 min) | Active | See the time left; cancel the request. |
| UNLOCK WINDOW (10 min) | Active | Turn off (with one confirmation); cancel. |
| OFF | Inactive | Turn on (instant). |

### 5.2 Rules

- L-01. When first-time setup is complete, No Doom is ON.
- L-02. In ON, the only way to turn protection off is "Request to turn off", which starts a 60-minute countdown. Before it starts, one confirmation explains the 60 + 10 minute rule.
- L-03. Protection stays fully active during COUNTDOWN and UNLOCK WINDOW.
- L-04. The owner can cancel at any time during COUNTDOWN or UNLOCK WINDOW, returning to ON instantly. A cancelled countdown gives no credit toward the next one.
- L-05. After 60 minutes the state becomes UNLOCK WINDOW and the owner is notified. This must happen on time even if No Doom's screen is closed and the phone is locked; up to 1 minute late is acceptable.
- L-06. During UNLOCK WINDOW a "Turn off No Doom" button is available. Pressing it and confirming moves to OFF.
- L-07. If 10 minutes pass without turning off, the state returns to ON and the owner is told. The next request starts a fresh 60-minute countdown.
- L-08. Only one countdown runs at a time. Requesting again during COUNTDOWN neither restarts nor shortens it.
- L-09. In OFF, No Doom does nothing inside Instagram and does not guard Settings (section 6). OFF lasts through reboots and app updates until the owner taps "Turn on".
- L-10. "Turn on" is instant, needs no confirmation and goes straight to ON.
- L-11. In the protected states, protection comes back by itself after a reboot, a No Doom or Instagram update, or the system stopping No Doom, with no owner action and no prompt. After a reboot it must be in force before the owner can use Instagram (see also R-18).
- L-12. Changing the phone's date, time or time zone never shortens or skips the countdown or the window.
- L-13. A reboot during COUNTDOWN or UNLOCK WINDOW cancels the request and returns to ON (assumption, see Q1). A No Doom or Instagram update is not a reboot and does not affect the countdown.
- L-14. Fail safe: if No Doom's saved state is missing, unreadable or inconsistent, it treats the state as ON. It never falls back to OFF by accident.
- L-15. No other setting can weaken protection. Any future setting that loosens protection must go through the same 60 + 10 minute lock; settings that tighten it apply instantly.
- L-16. Release builds contain no hidden, debug or developer way to skip or shorten the lock.

## 6. Self-protection

In the protected states, the 60-minute lock must be the only quick way to switch No Doom off. For each route below, the owner is moved out of that screen and sees the message in section 7.4 pointing them to the lock. In OFF, none of these guards apply and everything is allowed.

| # | Route that must be blocked | Notes |
| --- | --- | --- |
| S-01 | Turning off No Doom's Accessibility permission | Every path to it: Settings menus, Settings search, links from other apps. Includes the accessibility shortcut options on No Doom's page. |
| S-02 | Force stop on No Doom's App info |  |
| S-03 | Clear storage, clear cache or "Manage space" for No Doom | L-14 still applies if it ever happens. |
| S-04 | Uninstalling No Doom from anywhere | Settings app list, App info, home-screen long-press, drag to Uninstall, app drawer, any app store or file manager. |
| S-05 | Removing any extra right No Doom uses for uninstall protection | Only if such a right is used. |
| S-06 | Stopping No Doom from Developer options (Running services) or any other system screen that stops apps |  |
| S-07 | Pausing or restricting No Doom through Digital Wellbeing (app timers, Focus mode, Bedtime) or setting its battery use to Restricted | Must be blocked only if it would stop protection. If protection keeps working, these may be allowed. |

- S-08. Only No Doom's own pages are guarded. The rest of Settings, other apps' App info pages and other apps' accessibility services (password managers and so on) must keep working normally.
- S-09. The guards must never trap the owner: Back, Home and Recents always work, and the phone's emergency features are never blocked.
- S-10 (should). If protection was switched off by a route No Doom could not block (see section 12), the main screen shows it the next time No Doom runs, for example "Protection was interrupted on 16 Nov at 21:40".

## 7. App screens, notifications and messages

No Doom has a first-time setup flow and one main screen; everything else is a notification or a short message. The visual design is the developer's choice: plain, calm, readable, and following the phone's light or dark theme. All wording below is a suggestion for the owner to approve.

### 7.1 First-time setup

1. Welcome: what No Doom does in three or four short lines, with the 60 + 10 minute rule stated plainly so the owner knows what they are agreeing to.
2. Grant the Accessibility permission, with clear step-by-step help, including Android's "restricted setting" step if the phone shows it.
3. Allow notifications, needed to announce the unlock window.
4. Turn on uninstall protection, if the chosen design needs a separate step for S-04 and S-05.
5. Live check: the owner opens Instagram, opens any reel and tries to swipe, then taps the Reels tab. No Doom confirms it recognised both and blocked them.
6. Done: No Doom is ON.

If setup is left unfinished, the main screen shows what is missing and resumes from there. If a required permission is later lost, the main screen says so clearly and links to the fix.

### 7.2 Main screen

| State | Shows | Buttons |
| --- | --- | --- |
| ON | "No Doom is ON" and the health check | Request to turn off |
| COUNTDOWN | Time left (minutes:seconds) and the clock time it ends | Cancel request, stay protected |
| UNLOCK WINDOW | Time left in the window | Turn off No Doom (then confirm); Cancel |
| OFF | "No Doom is OFF" | Turn on |

The health check appears in every state:

- Accessibility permission: granted or missing.
- Instagram: installed version.
- Last time the reel viewer and the Reels tab were recognised, or a warning if Instagram has updated and they have not been recognised since (I-08).
- Any interruption found under S-10.

An About section shows No Doom's version and the rules in one short paragraph. There are no other settings (L-15).

### 7.3 Notifications

| When | Suggested text | Style |
| --- | --- | --- |
| COUNTDOWN running | "No Doom can be turned off at 21:40. Tap to cancel." | Silent, ongoing. Must not wake the phone to update a ticking number. |
| Window opens | "You can turn off No Doom in the next 10 minutes." | Normal alert (phone's sound/vibration settings). |
| Window missed | "Unlock window missed. No Doom stays ON." | Silent. |
| Instagram not recognised after an update | "Instagram changed. No Doom may not catch Reels until it is updated." | Silent, once per Instagram version. |

No other notifications: no permanent "No Doom is running" notification while ON, no reminders while OFF, no daily nags or stats.

### 7.4 On-screen messages

| Situation | Suggested text | Behaviour |
| --- | --- | --- |
| Reels tab blocked | "Reels tab is off" | Brief (about 2 seconds), does not block taps, not repeated within a few seconds. |
| Swipe blocked | "One reel only" or that save your brain from doom (prefered) | Brief and subtle; at most once every few seconds (see Q5). |
| Guarded Settings or uninstall screen | "No Doom is protecting you. To turn it off, request it in the No Doom app (60-minute wait)." there should be option to disable any always showing notification | Shown after the owner is moved out of the guarded screen, with an "Open No Doom" button. |
| Fallback cover (R-11 only) | "One reel only" | Covers the screen only for the instant needed. |

## 8. Battery, permissions, performance and privacy

No Doom must cost the owner nothing they can notice: no battery drain, no lag in Instagram, no internet access, and only the permissions listed here.

### 8.1 Permissions

| Permission | Status | Why |
| --- | --- | --- |
| Accessibility service | Allowed | Core function: recognise Instagram's screens, enforce section 4, guard No Doom's own Settings pages (section 6). |
| Notifications | Allowed | Announce the countdown, the unlock window and Instagram-compatibility warnings. |
| A right that protects against uninstalling (for example device admin) | Allowed only if needed | Only if S-04 and S-05 cannot be met without it. The developer explains why. |
| Anything needed to meet L-05 or L-11 on time (scheduling, start after reboot) | Owner approval first | Only if those rules cannot be met without it. |
| Internet or any network access | Not allowed | Nothing may leave the phone. |
| Display over other apps | Not allowed |  |
| Storage, photos, media, files | Not allowed |  |
| Contacts, location, camera, microphone, phone, SMS, calendar | Not allowed |  |
| Usage access, notification access, "see all apps" | Not allowed |  |
| Ignore battery optimisation (unrestricted background) | Not allowed |  |
| Permanent background service with an always-on notification | Not allowed |  |
| Analytics, crash-reporting, advertising or tracking libraries | Not allowed |  |

Any permission not on this list needs the owner's written approval and a stated reason.

### 8.2 Battery and resources

- N-01. No Doom does no work while neither Instagram nor a guarded Settings or uninstall screen is on screen. That means no polling, no ticking timers, no background jobs, no keeping the phone awake and no syncing. The only scheduled events are the end of the countdown and the end of the window.
- N-02. Battery: No Doom should not appear in the phone's battery usage list in normal use, and must stay under 1% of the day's battery on a day with 2 hours of Instagram use.
- N-03. Memory should stay small while No Doom's own screen is closed; the developer measures and reports it. App size should be under 10 MB.
- N-04. With the screen off and the phone idle overnight, No Doom causes no wake-ups.

### 8.3 Performance and glitches

- N-05. No added lag. Scrolling in the feed, DMs, Explore and profiles must look and feel identical with No Doom ON and OFF, compared side by side.
- N-06. No Doom never causes flicker, phantom taps, screen jumps, the keyboard closing, videos restarting or lost typing anywhere outside a blocked swipe.
- N-07. No false blocks: across the whole test pass, zero blocked swipes or redirects in the stories viewer, feed, DMs, Explore, profiles or comments.
- N-08. Instagram must never crash, freeze or sign the owner out because of No Doom.
- N-09. Reaction-time targets are as set in R-07, R-10 and R-11.

### 8.4 Reliability

- N-10. Protection survives reboots, updates of No Doom or Instagram, low memory, Battery Saver, the phone left idle for days, airplane mode and no network.
- N-11. If No Doom crashes, it recovers by itself and protection resumes with no owner action. Repeated crashes block a release, because Android may mark a crashing accessibility service as not working, which silently ends protection.

### 8.5 Privacy and security

- N-12. Nothing No Doom sees ever leaves the phone. It has no accounts, no cloud backup of its data and no network access.
- N-13. No Doom reads screen content only from Instagram and the system screens needed for section 6. It ignores every other app.
- N-14. Release builds store and log no screen content (usernames, captions, messages). Only the state, its timestamps and the Instagram version may be stored.
- N-15. The description shown on No Doom's Accessibility page honestly says what No Doom does and that nothing leaves the phone.

### 8.6 Compatibility

- N-16. Target: the owner's Pixel 9 Pro on the Android version it runs at delivery, with the Play Store Instagram current at delivery. It should keep working on the next Android release without changes.
- N-17. Language, font size, display size, theme and navigation mode: as in R-19.

## 9. Installation, updates and maintenance

No Doom is installed straight onto the owner's phone, not from the Play Store, and must be quick to fix whenever Instagram changes its screens.

### 9.1 Installation

- I-01. The developer delivers a signed release build plus an install guide written for the owner in plain language. It covers Android's warnings for apps from outside the Play Store, the "restricted setting" step for Accessibility, any Play Protect prompt, and first-time setup.
- I-02. Installing needs no rooting, no bootloader unlocking and no wiping of the phone.
- I-03. If Android or Google rules affect installing on the owner's phone (for example developer verification for apps from outside the Play Store, or Advanced Protection mode), the developer documents the path that works.

### 9.2 Updating No Doom

- I-04. A new version installs over the old one without uninstalling, without turning protection off, and without losing the state or a running countdown.
- I-05. The signing key and its passwords belong to the owner and are handed over with the source code. Without them, future updates cannot be installed over the existing app.
- I-06. Every release has a version number and a one-line change note.

### 9.3 When Instagram changes

- I-07. Instagram updates often. Everything No Doom relies on to recognise Instagram's screens is kept in one clearly documented place, so a fix after an Instagram update is a small, quick change.
- I-08. No Doom notices when it has probably stopped recognising Instagram (for example, Instagram's version changed and the reel viewer or Reels tab has not been recognised during normal use since) and tells the owner (sections 7.2 and 7.3).
- I-09. The developer delivers a maintenance guide: how to check a new Instagram version, find what changed, update, rebuild, rerun the section 11 checklist and install.
- I-10. The owner can capture a diagnostic snapshot to send to the developer. It is taken only when the owner asks, stays on the phone until the owner shares it manually, leaves out message and caption text, and never weakens protection.

## 10. Milestones and deliverables

There are five milestones, done in order. M0 is a go/no-go gate: it decides the swipe behaviour (R-10 or R-11), and the owner approves it before any further work.

1. **M0: Feasibility (gate).** Show that No Doom can recognise the reel viewer from every entry point in 4.2, and the Reels tab, on the owner's phone with current Instagram. Try both R-10 and R-11.
   - Delivers: a short report covering what works, what doesn't, the recommended swipe behaviour with screen recordings of each option, any screens that need the R-12 last resort, and risks found.
   - Done when: the owner approves the report.
2. **M1: Reel blocking.** All of section 4.
   - Done when: every section 4 test in the checklist passes (T-01 to T-28).
3. **M2: The lock.** All of section 5, the main screen (7.2) and notifications (7.3).
   - Done when: T-29 to T-41 pass.
4. **M3: Self-protection and setup.** Section 6, first-time setup (7.1) and on-screen messages (7.4).
   - Done when: T-42 to T-49 pass.
5. **M4: Hardening and handover.** The section 8 targets measured, the section 9 deliverables, and the full checklist passed on the owner's phone.
   - Done when: every item in section 11 passes, including the one-week trial (T-54).

**Final deliverables**

- Source code in a git repository the owner controls, with build instructions.
- Signed release build, plus the signing key and its passwords (I-05).
- Install guide for the owner (I-01).
- Maintenance guide for a developer (I-09).
- Test report: every checklist item with pass or fail and the date, phone model, Android version and Instagram version tested, plus screen recordings of the key tests (a blocked swipe from each entry point, the Reels tab, the full lock cycle).
- Battery and memory measurements (N-02, N-03).
- The permission list as shipped, matching 8.1.

## 11. Acceptance test checklist

No Doom is accepted when every item below passes on the owner's Pixel 9 Pro with the Instagram version current at handover. The test report records the date, Android version and Instagram version for each pass. Unless a test says otherwise, run it with No Doom ON.

### Reel viewer (4.1, 4.2, 4.4)

- [ ] T-01. Home feed: tap a reel, then swipe up 5 times. The viewer never leaves the starting reel and no other reel is heard.
- [ ] T-02. Repeat T-01 with swipes down, fast flicks, slow drags and swipes started near the screen edge.
- [ ] T-03. A reel sent in a one-to-one DM and in a group DM: one reel only.
- [ ] T-04. A story containing a reel: open the reel, swipe is blocked; back returns to the story; swiping between stories still works.
- [ ] T-05. Explore: the grid scrolls; a reel gives one reel only; a photo opens a post list that scrolls normally; a reel inside that list gives one reel only.
- [ ] T-06. Reels grid on the owner's profile and on another account's profile: one reel only.
- [ ] T-07. Search results, an audio page, a hashtag page and a saved collection: one reel only.
- [ ] T-08. An Instagram notification about a reel: one reel only.
- [ ] T-09. A reel link opened from WhatsApp and from Chrome: one reel only.
- [ ] T-10. With Instagram's auto-scroll setting on, and while watching a reel to the end 5 times, the viewer never advances.
- [ ] T-11. Every action in R-03 works on the starting reel.
- [ ] T-12. The back gesture, the 3-button Back and Instagram's back arrow each return to the right place.
- [ ] T-13. Leaving a reel and tapping a different one plays the new reel (chaining allowed).
- [ ] T-14. In split screen with another app, the rules still apply.
- [ ] T-15. Only if R-11 is used: the next reel is never heard and is covered within about 0.15 s, checked on a slow-motion screen recording.

### Reels tab (4.3)

- [ ] T-16. Tapping Reels lands on Home, no reel is heard, and the message shows once.
- [ ] T-17. Tapping Reels 10 times quickly never stays on Reels, does not spam messages and does not crash.
- [ ] T-18. After closing Instagram from recent apps and reopening it, or using any Reels shortcut or deep link, Instagram never lands on the Reels tab.

### Everything else untouched (4.5)

- [ ] T-19. Scrolling the home feed for 5 minutes, including inline reels and suggested-reels rows: no interference.
- [ ] T-20. DMs: the chat list and long chats scroll normally; typing, voice notes and sending photos work.
- [ ] T-21. Stories: tapping through, swiping between people and replying all work.
- [ ] T-22. The Explore grid, profiles, comments, activity, Instagram settings, camera and posting all work normally.
- [ ] T-23. Side-by-side comparison of scrolling with No Doom ON and OFF: no visible difference in smoothness.
- [ ] T-24. Other apps are unaffected. Settings pages other than No Doom's work normally, including other apps' accessibility services.

### Timing and coverage (4.6)

- [ ] T-25. Cold-start Instagram, open a reel immediately and swipe: blocked on the first try. Reboot, unlock and open Instagram as fast as possible: protection is already in force.
- [ ] T-26. Turning No Doom on while a reel viewer is open makes the current reel the starting reel; turning it on while the Reels tab is open sends Instagram to Home.
- [ ] T-27. With Instagram in another language, the largest font and display size, dark and light theme, and gesture and 3-button navigation, T-01 and T-16 still pass.
- [ ] T-28. After switching Instagram accounts, the rules still apply.

### The lock (5)

- [ ] T-29. From ON, "Request to turn off" shows the confirmation, then a countdown of about 60:00 with its end time; protection stays active.
- [ ] T-30. Cancelling during the countdown returns to ON; the next request starts at 60:00.
- [ ] T-31. Requesting again during the countdown neither restarts nor shortens it.
- [ ] T-32. Waiting 60 minutes with the phone locked and No Doom closed: the window notification arrives within 1 minute of the right time.
- [ ] T-33. Turning off inside the window moves to OFF: Instagram behaves normally and guarded screens open freely.
- [ ] T-34. Letting the window expire returns to ON with a notification; the next request starts at 60:00.
- [ ] T-35. Cancelling inside the window returns to ON.
- [ ] T-36. Changing the date, time or time zone during the countdown and during the window has no effect on time left.
- [ ] T-37. A reboot during the countdown and during the window returns to ON (L-13).
- [ ] T-38. OFF stays OFF after a reboot; ON comes back ON after a reboot with no owner action.
- [ ] T-39. "Turn on" from OFF is instant.
- [ ] T-40. Installing a new No Doom build over the old one, in each state, keeps the state; a running countdown continues.
- [ ] T-41. A simulated lost or corrupted state (developer test build) comes up ON.

### Self-protection and setup (6, 7.1); run T-42 to T-47 in ON, COUNTDOWN and UNLOCK WINDOW

- [ ] T-42. No Doom's Accessibility switch, reached through the Settings menus and through Settings search, is blocked (S-01).
- [ ] T-43. Force stop, Clear storage, Clear cache and Uninstall on No Doom's App info are blocked (S-02 to S-04).
- [ ] T-44. Uninstalling by home-screen long-press, drag to Uninstall and the app drawer is blocked (S-04).
- [ ] T-45. Removing the uninstall-protection right (if used) and stopping No Doom from Developer options are blocked (S-05, S-06).
- [ ] T-46. Digital Wellbeing app timer, Focus mode and Bedtime on No Doom, battery set to Restricted, and Battery Saver: protection keeps working, or the route is blocked (S-07).
- [ ] T-47. Back, Home and Recents always work, emergency calling stays reachable, and other apps' settings are untouched (S-08, S-09).
- [ ] T-48. In OFF, every route above is allowed and uninstalling works cleanly.
- [ ] T-49. First-time setup on a fresh install works end to end, including the live check (7.1).

### Battery, privacy and long run (8, 9)

- [ ] T-50. A day with 2 hours of Instagram: No Doom uses under 1% of battery and ideally does not appear in the list.
- [ ] T-51. Idle overnight with the screen off: no wake-ups from No Doom. Memory use is measured and reported.
- [ ] T-52. The installed app's permissions match 8.1 exactly, with no network access.
- [ ] T-53. A simulated unrecognised Instagram (developer test) shows the warning once (I-08).
- [ ] T-54. One-week trial by the owner: no glitches, no second reel reached by swiping, no false blocks.

## 12. Out of scope, accepted limitations and risks

No Doom is built to beat the impulse to scroll, not to stop a determined owner; the owner accepts the limits below.

**Out of scope for version 1** (possible later)

- Instagram in a web browser, Instagram Lite, and any other Instagram app.
- Facebook Reels, YouTube Shorts, TikTok and similar feeds in other apps.
- Other phones, tablets and iPhone.
- Usage statistics, time tracking, streaks and reminders.
- Play Store publishing, multiple phone users, and cloud backup.

**Accepted limitations**

- Someone determined can still switch No Doom off quickly with a computer (developer tools), by booting into safe mode or by factory-resetting the phone, or can use Instagram on another device or the web.
- Opening reels one at a time by tapping (from grids, profiles, audio pages) is allowed by design.
- If the R-11 fallback is used, a fraction of a second of the next reel may be visible, though never heard.
- After an Instagram update that changes its screens, protection may be partly broken until No Doom is updated. No Doom says so when this happens (I-08).

**Risks**

| Risk | Effect | Expected from the developer |
| --- | --- | --- |
| Instagram changes its screens in an update | Recognition breaks | I-07 to I-09: a small, documented, quick fix. |
| Android offers no clean way to make a swipe do nothing inside another app | R-10 not achievable | Prove it either way in M0; fall back to R-11. |
| Android or Google tighten rules for accessibility apps installed outside the Play Store (restricted settings, Advanced Protection, Play Protect, developer verification) | Installing or granting the permission gets harder | Document the working path (I-03). |
| Meta notices an accessibility app running | Very low account risk, because No Doom sends nothing and automates nothing beyond D3 | Keep strictly to D3. |
| Android marks No Doom as "not working" after repeated crashes | Protection silently ends | N-11: crash-free releases; the health check shows it. |
| Instagram installed in Private Space or a work profile | Not covered | See Q4. |

## 13. Open questions for the owner

The assumptions in brackets apply unless the owner decides otherwise before M1.

- [ ] Q1. Should a reboot during the countdown or the unlock window cancel the request, or should the countdown keep running through the reboot? (Assumed: cancel and return to ON, as in L-13.)
- [ ] Q2. If an Instagram update breaks recognition, should No Doom only warn you, or block Instagram entirely until it is updated? (Assumed: warn only.)
- [ ] Q3. Instagram Live: if swiping in a Live video moves to another Live, should that be blocked like reels? (Assumed: yes.)
- [ ] Q4. Should No Doom also stop Instagram installed in Private Space or a work profile, and Instagram Lite? (Assumed: out of scope for version 1.)
- [ ] Q5. On a blocked swipe, show the brief "One reel only" hint or block silently? (Assumed: show it briefly.)


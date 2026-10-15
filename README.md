# TV Remote (Android)

A phone remote for Android TV / Google TV. Original code and design, written against the Android TV Remote v2 protocol
(pairing on port 6467, remote on 6466, TLS with a client certificate, protobuf messages).

**Features:** pairing with the on-screen code, D-pad, touch mode, keypad, VOL / CH, Mute, power, Apps grid
(Airtel Xstream, ZEE5, JioHotstar, Netflix, Prime Video, Apple TV+, YouTube) with custom apps and custom links,
and a keyboard tab that opens by itself when a text field gets focus on the TV.

Brand logos are from Wikimedia Commons. Trademarks belong to their owners.

## How it was made
This app was designed, coded, built and tested entirely by an AI agent (Instinct), working with its owner over chat, in about an hour:
scoping, ten rounds of design mocks, the Android TV Remote v2 protocol layer, the UI, the signed APK and this repo.
No Gradle, no libraries: plain Java and Android views, built with the Android SDK command-line tools.

**Tested on a real TV:** the owner paired it with his Android TV and confirmed connection and remote keys working.
Netflix, YouTube and Prime Video tiles launch on it; the Xstream and ZEE5 tiles depend on the TV's installed package names (editable in the app with a long-press).

## Design

![Overview of all screens](mocks/v11-overview.png)

| Add your TV (pairing) | Remote | Touch mode |
|---|---|---|
| ![Pairing](mocks/v10-1-pairing-steps.png) | ![Remote](mocks/v10-2-remote.png) | ![Touch mode](mocks/v10-3-touch-on.png) |

| Keypad | Apps | Keyboard |
|---|---|---|
| ![Keypad](mocks/v10-4-keypad.png) | ![Apps](mocks/v11-5-apps.png) | ![Keyboard](mocks/v10-6-keyboard.png) |

| No TV found | Add custom app | Add custom link |
|---|---|---|
| ![No TV](mocks/v10-7-no-tv-found.png) | ![Custom app](mocks/v11-8-custom-app-tab.png) | ![Custom link](mocks/v11-9-custom-link-tab.png) |

These are design mocks. Differences in the built app: the "suggestions" list under Add custom app is not built, and the
"app isn't installed" banner is not shown (the protocol gives no way to know). Pairing shows the code box only after you tap Connect and the TV shows its code.

## Changes in v1.1
Opens on the Remote tab (a TV focus event right after connecting no longer jumps to Keyboard); swipe between D-pad and keypad works anywhere on the page;
keypad digits clear after 2.5 s idle; bigger VOL / CH pills (+ top, - bottom, live volume number in the VOL pill), Back/Home/Menu and Mute in the middle column;
app tiles try deep links first (Netflix, YouTube, Prime Video), tap again within 10 s to try the next way, corrected package names (Xstream `tv.airtel.xstream.tvapp`, JioHotstar `in.startv.hotstartvonly`),
and the app shows a message with the exact link it sent and when the TV rejects a command.
The remote-screen mocks above show the v1.0 layout; the built v1.1 layout differs as described.

## Changes in v1.2
Keyboard auto-open no longer fires for focus events the TV raises by itself: it is suppressed for 5 s after any remote key (not D-pad/OK/Enter/Delete) and 10 s after an app launch, as well as 4 s after connecting and when the focused package is a launcher. A later real tap into a TV text field still opens it.
Menu button removed (Back and Home are wider, Mute is bigger); the "Mute" caption is gone. Xstream tile tries `com.airtel.tv` (the Xstream app on Airtel's IPTV box) then `tv.airtel.xstream.tvapp`; every tile now ends with a Play Store page fallback. ZEE5 has no known alternate package, so it falls back to its store page.

## Build
No Gradle. Needs JDK 17 and the Android SDK (platform 34, build-tools 34.0.0) at the paths in `build.sh`.
`./build.sh` produces a signed `build/TVRemote.apk` (it creates a keystore on first run; keep it so later builds can update the installed app).

## Tests
- `test/run_protocol_test.sh`: the Java pairing and remote client against a mock TV (Python, built on the open-source `androidtvremote2` protobuf definitions as an independent check of the wire format).
- `test/UnitTests.java`: text diff, custom app storage, link and package validation.
- `test/ui/run_ui_test.sh`: Robolectric run of the real activity against the mock TV (pairing, wrong code, d-pad, touch pad, keypad, bottom buttons, apps, custom link, keyboard auto-open and live typing).

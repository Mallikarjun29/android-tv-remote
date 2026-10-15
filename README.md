# TV Remote (Android)

A phone remote for Android TV / Google TV. Original code and design, written against the Android TV Remote v2 protocol
(pairing on port 6467, remote on 6466, TLS with a client certificate, protobuf messages).

**Features:** pairing with the on-screen code, D-pad, touch mode, keypad, VOL / CH, Mute, power, Apps grid
(Airtel Xstream, ZEE5, JioHotstar, Netflix, Prime Video, Apple TV+, YouTube) with custom apps and custom links,
and a keyboard tab that opens by itself when a text field gets focus on the TV.

Brand logos are from Wikimedia Commons. Trademarks belong to their owners.

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

## Build
No Gradle. Needs JDK 17 and the Android SDK (platform 34, build-tools 34.0.0) at the paths in `build.sh`.
`./build.sh` produces a signed `build/TVRemote.apk` (it creates a keystore on first run; keep it so later builds can update the installed app).

## Tests
- `test/run_protocol_test.sh`: the Java pairing and remote client against a mock TV (Python, built on the open-source `androidtvremote2` protobuf definitions as an independent check of the wire format).
- `test/UnitTests.java`: text diff, custom app storage, link and package validation.
- `test/ui/run_ui_test.sh`: Robolectric run of the real activity against the mock TV (pairing, wrong code, d-pad, touch pad, keypad, bottom buttons, apps, custom link, keyboard auto-open and live typing).

## Not verified without a real TV
Real pairing and control, mDNS discovery on a real network, keyboard auto-open on a real TV, whether each app opens
(package names are best guesses, editable in the app with a long-press), real Wi-Fi behaviour, and how the UI looks and feels on a real phone.
Checklist: `docs/TV_TEST_CHECKLIST.md`.

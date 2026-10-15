# TV Remote (Android)

A phone remote for Android TV / Google TV. Original code and design, written against the Android TV Remote v2 protocol
(pairing on port 6467, remote on 6466, TLS with a client certificate, protobuf messages).

Features: pairing by on-screen code, D-pad, touch mode, keypad, VOL/CH, Mute, power, Apps grid (Airtel Xstream, ZEE5,
JioHotstar, Netflix, Prime Video, Apple TV+) with custom apps and links, keyboard that opens automatically when a TV text field gets focus.

Brand logos are from Wikimedia Commons; trademarks belong to their owners.

## Build
No Gradle. Needs JDK 17 and Android SDK (platform 34, build-tools 34.0.0) at the paths in `build.sh`. `./build.sh` makes a signed `build/TVRemote.apk`
(it creates a keystore on first run; keep it to be able to update the app later).

## Tests
- `test/run_protocol_test.sh`: runs the Java protocol layer against a mock TV (Python, uses the open-source `androidtvremote2` protobuf definitions as an independent oracle).
- `test/UnitTests.java`: text diff, custom app storage, link validation.

## Design
`mocks/` has the v10 screen mocks.

## Not verified without a real TV
Real pairing and control, mDNS discovery on a real network, keyboard auto-open on a real TV, app launch by package name (package names are best guesses and editable in-app).

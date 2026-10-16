# Security policy

## Reporting a problem

Please report security problems privately through GitHub: open the **Security** tab of this repository and choose **Report a vulnerability**. If that button is not available, open an issue that says you have a security report, without technical details, and the maintainer will arrange a private channel.

This is a small, volunteer-run hobby project. There is no bounty and no guaranteed response time, but reports are read and taken seriously.

## What the app does and does not protect

- The app talks only to the Android TV on your local network, over TLS, on ports 6466 and 6467. It opens no listening ports and has no accounts, servers or analytics.
- Pairing uses the code shown on the TV. The pairing key and the favourites stay on the phone. The key is not included in Android backups.
- From v1.8 the app remembers the TV's certificate fingerprint after the first successful connection and refuses a different device at the same address. Installs from before v1.8 pin the TV the first time they connect after updating.
- Any phone on the same Wi-Fi can ask your TV to pair, and a paired phone has full control until you remove it on the TV. That is how the Android TV remote protocol works, not something this app can change.

## Verifying a download

Every release lists the APK's SHA-256 and the SHA-256 of the signing certificate. All releases are signed with the same key, so a genuine update installs over the old version. If the signing certificate does not match, do not install it.

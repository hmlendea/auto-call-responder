# Quick start

This guide takes a newcomer from a fresh checkout to a running, configured application.

## Prerequisites

- JDK 17.
- Android SDK with the API 36 platform and build tools.
- A `local.properties` file that declares the SDK location through `sdk.dir`.
- A physical device that can receive calls (an emulator cannot exercise the telephony path meaningfully).

## Build and install

```bash
git clone https://github.com/hmlendea/auto-call-responder.git
cd auto-call-responder
echo "sdk.dir=/path/to/Android/Sdk" > local.properties
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## First-run configuration

1. Launch **Auto Call Responder**.
2. Tap **Settings**.
3. Set the **delay before answering** with the seek bar (1–60 seconds).
4. Set the **delay after answering before audio** (0–60 seconds).
5. Select the audio file to play with **Select Audio File**; the file is copied into application storage.
6. Enter the notification **e-mail address** and, optionally, the SMTP host, port, user name, and password.
7. Choose whether to **play through the earpiece only** and whether to **filter Romanian numbers only**.
8. Tap **Save**.
9. Return to the main screen, enable the responder with the switch, and tap **Request Permissions**.
10. Grant the runtime permissions and assign the dialer and call-screening roles when prompted.
11. Optionally grant root access and disable battery optimisation from the settings screen.

## Verify

- The main screen shows **Status: Enabled**, the effective delay, the recipient address, and the audio selection state.
- Tap **Test Email** to exercise the SMTP settings against the configured server.
- Place a call from an eligible number and confirm that it is answered after the delay, that the audio plays, and that the notification e-mail arrives.

## Run the automated checks

```bash
./gradlew assembleDebug   # debug build
./gradlew test            # unit tests
./gradlew lint            # Android lint
```

## Where to go next

| If you want to… | Read |
|-----------------|------|
| Understand the whole system | [architecture.md](./architecture.md) then root [ARCHITECTURE.md](../ARCHITECTURE.md) |
| Trace a call end to end | [flows/dialer-call-handling.md](./flows/dialer-call-handling.md) |
| Change the answering sequence | [change-guide.md](./change-guide.md) and [invariants.md](./invariants.md) |
| Diagnose a failure | [troubleshooting.md](./troubleshooting.md) and [logging.md](./logging.md) |
| Understand the settings schema | [configuration.md](./configuration.md) |

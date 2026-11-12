# Repository overview

## Purpose

Auto Call Responder is a device-local Android application whose single purpose is to answer selected incoming calls without user interaction, play a local audio recording during the answered call, keep the microphone muted for the rest of the call, end the call when playback finishes, and send an owner e-mail that describes the call. It exists to give a device owner an automated, unattended way to pick up calls and play a message.

## Scope

The repository owns:

- The call-observation decision (which calls are eligible and when they are handled).
- The ordered answering, muting, playback, disconnecting, and notification sequence.
- The settings model, its serialisation, and the imported audio file.
- The composition of the call notification e-mail and the SMTP transport that sends it.
- Optional root detection and the audio-mixer commands that attempt to route playback into the voice uplink.

## Deliberate non-scope

The repository does not own:

- The Android call stack, the Telecom framework, or the telephony radio.
- The SMTP server or the delivery of the message beyond its hand-off.
- The device audio hardware, the kernel mixer, or the root implementation.
- Any server, scheduler, dashboard, or remote control plane. There is no backend.
- Any analytics, telemetry, crash reporting, or update mechanism.

## Primary capabilities

| Capability | Owner | Entry point |
|------------|-------|-------------|
| Automatic answering after a configurable delay | `AutoCallInCallService`, `CallHandlingService` | Telecom bind / foreground-service start |
| Romanian-number eligibility filter | `RomanianPhoneNumbers` | Called by both call paths |
| Audio playback during the call | `AutoCallInCallService`, `CallHandlingService` | Scheduled after the answer settles |
| Microphone mute for the whole call | `CallAudioController` | `muteMicrophone` / `unmuteMicrophone` |
| Audio route selection (earpiece or speaker) | `CallAudioController` | `prepareForPlayback` |
| Root-assisted uplink routing | `CallAudioController`, `RootShell` | `applyRootUplinkRouting` |
| Notification e-mail after the call | `EmailNotifier`, `EmailSender` | `sendCallNotification` |
| Settings persistence | `SettingsManager` | Shared preferences |
| Audio-file import | `AudioFilePicker` | Document picker result |

## Entry points

The application has no network endpoint. Its entry points are Android component callbacks:

| Entry point | Trigger | Declared in |
|-------------|---------|-------------|
| `MainActivity` | Launcher tap; `DIAL` intent | `AndroidManifest.xml` |
| `SettingsActivity` | Settings button | `AndroidManifest.xml` |
| `CallScreeningService` | Telecom screening request | `AndroidManifest.xml` |
| `AutoCallInCallService` | Telecom in-call binding | `AndroidManifest.xml` |
| `PhoneStateReceiver` | `android.intent.action.PHONE_STATE` | `AndroidManifest.xml` |
| `BootReceiver` | `BOOT_COMPLETED`, `QUICKBOOT_POWERON`, `MY_PACKAGE_REPLACED` | `AndroidManifest.xml` |
| `CallHandlingService` | Explicit `startForegroundService` from the receiver | `AndroidManifest.xml` |
| `FileProvider` | Internal content resolution of the imported audio | `AndroidManifest.xml` |

## External boundaries

- **Android Telecom framework:** delivers screening and in-call callbacks, and accepts answer and disconnect requests.
- **Telephony framework:** delivers the legacy `PHONE_STATE` broadcast and read-only phone state.
- **Device user:** grants permissions, the dialer and call-screening roles, optional root, and the battery-optimisation exemption, and edits the settings.
- **SMTP server:** receives the notification message over STARTTLS or implicit TLS.
- **Root shell and audio mixer:** provide `su` and a mixer binary when the device is rooted.
- **Private application storage:** holds the settings JSON and the imported audio file.

## Related documents

- [Repository structure](./repository-structure.md)
- [Architecture](./architecture.md)
- [Quick start](./quick-start.md)
- Root [ARCHITECTURE.md](../ARCHITECTURE.md)

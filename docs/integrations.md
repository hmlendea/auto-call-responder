# Integrations

## Integration inventory

| Integration | Direction | Compatibility | Purpose | Required | Owner |
|-------------|-----------|---------------|---------|----------|-------|
| `CallScreeningService` (Android Telecom) | Inbound | Android 10+ with the call-screening role | Observe incoming calls and classify them. | No (only on the role path) | `CallScreeningService` |
| `InCallService` (Android Telecom) | Inbound | Android 10+ with the dialer role | Authoritative call handling and mute/route control. | No (only on the role path) | `AutoCallInCallService` |
| `TelecomManager.acceptRingingCall` / `endCall` | Outbound | Android 9 (API 28) or later | Answer and end calls on the legacy path. | Yes on the legacy path | `CallHandlingService` |
| `PHONE_STATE` broadcast | Inbound | Android 8–9, or any state without the dialer role | Observe call state on the legacy path. | Yes on the legacy path | `PhoneStateReceiver` |
| Boot broadcasts | Inbound | All supported versions | Restore readiness after reboot or update. | No | `BootReceiver` |
| SMTP server | Outbound | STARTTLS on the configured port, or implicit TLS on 465; sender `AUTH LOGIN` | Send the notification e-mail. | No | `EmailSender` |
| `su` shell | Outbound | Rooted device with a granted `su` prompt | Detect root and run privileged commands. | No | `RootShell` |
| Audio mixer (`tinymix`, `tiny-mix`, `/vendor/bin/tinymix`) | Outbound | Rooted device whose kernel exposes an uplink loopback control | Route playback into the voice uplink. | No | `CallAudioController` |
| `FileProvider` | Internal | All supported versions | Expose the imported audio to the media player. | Yes for playback | `AudioFilePicker`, `file_paths.xml` |
| Android `RoleManager` | Outbound | Android 10+ | Query and request the dialer and call-screening roles. | Yes on the role path | `MainActivity`, `BootReceiver`, `PhoneStateReceiver` |

## Android Telecom

Telecom binds `CallScreeningService` for the duration of a single `onScreenCall` call, and binds `AutoCallInCallService` for as long as the application holds the dialer role. Both bindings are released by the framework when the role is lost or the process ends. The in-call path registers a `Call.Callback` per call and receives `STATE_RINGING`, `STATE_ACTIVE`, and `STATE_DISCONNECTED` transitions; it can also receive repeated `STATE_RINGING` for the same call, which the session map suppresses.

## SMTP

The transport is a hand-written client that supports the minimum needed for a text message:

- `EHLO` with the local canonical host name;
- `STARTTLS` on any port other than 465;
- implicit TLS when the port is 465;
- `AUTH LOGIN` with base64 credentials;
- `MAIL FROM`, `RCPT TO`, `DATA` with a base64 body and CRLF-terminated lines;
- `QUIT`.

Any status other than the expected one raises `IOException`, which the notifier logs.

## Root and audio mixer

Root is used for three purposes:

1. detection (`su -c id`, checked for `uid=0`);
2. microphone mute fallback (`cmd audio set-mic-mute true|false`);
3. uplink routing through a mixer binary and a catalogue of Qualcomm/MediaTek control pairs.

Failure at any step is expected and degrades to local-only playback.

## File provider

The manifest declares `androidx.core.content.FileProvider` with authority `${applicationId}.fileprovider`, `exported="false"`, and `grantUriPermissions="true"`. `file_paths.xml` maps the name `call_audio` to the `call_audio/` path under `files-path`. `AudioFilePicker.import` returns the URI resolved against this authority.

## Explicit non-integrations

- No analytics, crash reporting, advertising, or update service.
- No cloud storage or synchronisation service.
- No calendar, contacts, or messaging integration.
- No third-party telephony or VoIP SDK.

## Related documents

- [Dependencies](./dependencies.md)
- [Security](./security.md)
- [Flow: notification delivery](./flows/notification-delivery.md)
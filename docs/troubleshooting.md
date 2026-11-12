# Troubleshooting

## Guidance by symptom

| Symptom | Probable cause | Evidence to collect | Resolution |
|---------|----------------|---------------------|------------|
| Calls are never answered | Dialer role not held on Android 10+ | `BootReceiver` log "Dialer role is not held"; `MainActivity` permission button visible | Assign the dialer role from the main screen or system settings. |
| Calls are answered but not observed | Call-screening role not held | `BootReceiver` log "Call-screening role is not held" | Assign the call-screening role. |
| Calls are not answered | Responder disabled | `AutoCallInCallService` logs nothing; `CallScreeningService` logs "Responder disabled" | Enable the switch on the main screen. |
| Calls are not answered | Number filtered out | `CallScreeningService` logs "Number is not Romanian" | Disable the Romanian filter or call from an eligible number. |
| Calls are answered twice | Legacy receiver and dialer path both ran | Two `CallHandlingService` and `AutoCallInCallService` sequences for one call | Confirm `ROLE_DIALER` is held; the receiver gates on it. |
| No audio plays | No audio selected or file missing after a backup restore | `AutoCallInCallService` log "No audio file configured"; playback error logs | Reselect the audio file. |
| Audio plays through the speaker | `earpieceOnly` disabled or `InCallService.setAudioRoute` failed | `CallAudioController` route logs | Enable earpiece-only and confirm the dialer role. |
| No notification e-mail | Recipient, user, or password blank | `EmailNotifier` log "Email settings are incomplete" | Complete the e-mail settings and use **Test Email**. |
| No notification e-mail | SMTP rejection or timeout | `EmailNotifier` log "Could not send call notification" with the server response | Verify host, port, credentials, and provider app-password requirements. |
| Test e-mail fails | Same as above | `MainActivity` log and toast text | Correct the settings. |
| Caller hears nothing | Uplink injection requires root and a supported mixer control | `CallAudioController` log "No root, keeping local playback only" or "Mixer exposed no uplink route" | Grant root; accept that local playback is the supported outcome. |
| Root status stays "not checked" | Detection has not run | `RootShell` log | Tap **Grant Root Access** or reboot with the responder enabled. |
| Handling is delayed | Battery optimisation | `BootReceiver` log "Battery optimisation is enabled" | Use **Disable Battery Optimization**. |
| No notification shown on Android 13+ | `POST_NOTIFICATIONS` not granted | Missing foreground notification | Grant the notification permission. |
| Caller number is empty | The device restricts caller identification | Log shows an empty number | Disable the Romanian filter if such calls must be handled. |
| Settings are lost | Application data cleared or the process restored from a backup | Preferences file absent | Reconfigure. |

## Diagnosing by log tag

| Question | Filter |
|----------|--------|
| Was the call observed? | `CallScreeningService` or `PhoneStateReceiver` |
| Which path handled it? | `AutoCallInCallService` (dialer) versus `CallHandlingService` (legacy) |
| Was it answered? | "Answered call from" or "Answered the ringing call" |
| Did audio play? | "Audio playback started" or "Audio playback completed" |
| Did playback fail? | "Audio playback failed" or "Could not play audio" |
| Did the mute apply? | `CallAudioController` "Microphone muted" |
| Was the notification attempted? | `EmailNotifier` success or failure message |
| Was root available? | `RootShell` "Root access available" |

## Edge cases

- **Call ends during the ringing delay (dialer path):** `STATE_DISCONNECTED` calls `finishSession`, which cancels the pending answer and sends a notification with `answeredAtMillis = 0`.
- **Call ends during the ringing delay (legacy path):** the receiver cancels the delayed start and sends `ACTION_STOP` with the number; the service shows the foreground notification, reports the ended call, and stops.
- **Backup restore without the audio file:** the audio URI is restored but the file is not; playback fails and the call still ends and notifies.
- **Root revoked while cached as granted:** `RootShell.isAvailable()` remains true until the next detection, so commands fail and are logged.

## Related documents

- [Logging](./logging.md)
- [Error handling](./error-handling.md)
- [FAQ](./faq.md)
- [Ambiguities and open questions](./ambiguities-and-open-questions.md)
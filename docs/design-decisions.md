# Design decisions

Each decision below is derived from the implementation. The rationale column records what the source comments, structure, and behaviour imply; it is not a record of a separate design meeting.

| Decision | Evidence in source | Rationale | Consequence |
|----------|--------------------|-----------|-------------|
| Prefer `InCallService` for the authoritative path | `AutoCallInCallService`, `CallAudioController.setAudioRoute`, comments in `CallAudioController` | `InCallService.setMuted` and `setAudioRoute` are the only supported APIs; `AudioManager` equivalents need the signature permission `MODIFY_AUDIO_ROUTING`. | The dialer path is authoritative and the legacy path is suppressed while the role is held. |
| Keep screening as classification only | `CallScreeningService` always responds with an empty `ALLOW_RESPONSE` | Telecom unbinds the service when `onScreenCall` returns, and a screening service may only allow, reject, or silence. | Screening never answers or plays audio; it logs the decision only. |
| Mute before answering and re-apply on `STATE_ACTIVE` | `AutoCallInCallService.answer`, `STATE_ACTIVE` branch, `MICROPHONE_MUTE_REAPPLY_MILLISECONDS` | Telecom clears the mute flag during the transition to active. | Two mute calls plus a scheduled safety task. |
| Unmute only during playback | `playAudio` / `playAudioFileAndAwait` unmute before `start`; completion and error mute again | Ambient sound must never reach the caller outside the recording. | Playback is explicitly bracketed by mute calls on both paths. |
| Persist an imported copy, not the document URI | `AudioFilePicker.import` copies into `filesDir/call_audio` | External document grants are transient and can be revoked. | The directory is purged on each import, so exactly one file is retained. |
| Notify only after disconnection | Both `notifyCallHandled` implementations are called after the disconnect attempt | The body needs the end timestamp, and SMTP latency must not delay the end. | Delivery is asynchronous and its outcome is not observed. |
| Fire-and-forget SMTP | `EmailNotifier.send` launches `CoroutineScope(Dispatchers.IO)` without awaiting | Notification is best-effort and must not block or crash call handling. | No retry, queue, or durable outbox exists. |
| Treat root as optional | `RootShell.isAvailable` returns false until detection completes; `CallAudioController.applyRootUplinkRouting` logs degradation | Most devices expose no uplink loopback control even when rooted. | Local playback remains intact when root or mixer controls are absent. |
| Single-call model | One `mediaPlayer` and one `audioController` field in `AutoCallInCallService`; one `activeSession` in `CallHandlingService` | The product assumes one incoming call at a time. | Concurrent calls are not modelled. |
| Gson for settings | `SettingsManager` serialises `AppSettings` to a JSON string | Minimal code for a flat settings record. | Field names are an implicit persistence contract with no schema version. |
| Plain-text SMTP client | `EmailSender` / `SmtpConnection` | Avoids a mailing library and works without user interaction. | The client supports only `AUTH LOGIN`, STARTTLS, implicit TLS on 465, and text bodies. |
| No `WorkManager` or `JobScheduler` | Only a foreground service and handlers/coroutines exist | Handling is tied to an active call and to short delays. | Aggressive battery optimisation can delay the answer; the application can only request an exemption. |

## Decisions that were considered and rejected by the implementation

| Alternative | Why it is not used |
|-------------|--------------------|
| `AudioManager.isMicrophoneMute` as the primary mute | It silently does nothing for a normal application in some Android versions; it remains only as a last-resort fallback. |
| `AudioManager.isSpeakerphoneOn` as the primary route | Same signature-permission limitation; it remains only on the legacy path. |
| Starting a service from `BootReceiver` | Telecom binds the services on its own when the roles survive the reboot. |
| A database for settings | The settings record is flat and small; shared preferences suffice. |
| Injecting audio into the uplink through public APIs | The uplink is a hardware capture path; this is impossible without root and kernel support. |

## Related documents

- [Ambiguities and open questions](./ambiguities-and-open-questions.md)
- [Invariants](./invariants.md)
- Root [ARCHITECTURE.md](../ARCHITECTURE.md)
# Error handling

## Philosophy

Every platform interaction that can fail is wrapped, logged, and allowed to degrade locally. The call sequence favours reaching the disconnect and notification steps over aborting. Errors that the user must resolve (missing roles, missing permissions, incomplete e-mail settings) are surfaced only as logs or toasts.

## Error taxonomy

| Category | Examples | Handling |
|----------|----------|----------|
| Prerequisite missing | Dialer role, screening role, `ANSWER_PHONE_CALLS`, `RECORD_AUDIO` | Logged (`Log.w`); the affected step is skipped. |
| Platform call failure | `Call.answer`, `Call.disconnect`, `TelecomManager.acceptRingingCall`, `TelecomManager.endCall` | Caught, logged (`Log.e`), sequence continues to the next step. |
| Audio failure | `MediaPlayer` prepare/start error, playback error callback, no audio configured | On the dialer path a missing file ends the call; a playback error completes the wait, mutes, and still disconnects. On the legacy path the `CompletableDeferred` completes on error. |
| File import failure | The selected document cannot be opened, provider unavailable | Returns `null`, shows a toast, leaves the previous audio selection unchanged. |
| Settings failure | Missing preferences, malformed JSON | A missing JSON returns defaults; a malformed JSON raises during Gson parsing and is not caught. |
| Root failure | `su` missing, prompt denied, timeout, mixer absent | `RootShell` returns `null` or `false`; the controller logs degradation and keeps local playback. |
| SMTP failure | Incomplete settings, connect failure, unexpected status line, timeout | `EmailNotifier` logs and abandons the message; `MainActivity` shows a toast. |
| Intent resolution failure | No document picker, donation page unavailable | Logged and a toast is shown. |
| Role request failure | `RoleManager` unavailable or exception | Logged (`Log.w`); on the dialer path a fallback opens the default-apps settings screen. |

## Failure propagation by component

| Component | Catches | Does not catch |
|-----------|---------|----------------|
| `MainActivity` | Donation intent, role requests, e-mail send via `runCatching` | Nothing else; the activity is a normal UI component. |
| `SettingsActivity` | Battery exemption intent | Nothing else. |
| `AudioFilePicker` | Picker launch and file import | Nothing else. |
| `CallScreeningService` | Nothing; it has no failure-prone call other than `respondToCall` | An exception in `respondToCall` would propagate to Telecom. |
| `AutoCallInCallService` | `call.answer`, `call.disconnect`, `MediaPlayer` prepare/start, player stop | `SettingsManager.getSettings` parsing. |
| `PhoneStateReceiver` | Service start for `ACTION_STOP` | Service start for `ACTION_START`. |
| `CallHandlingService` | Answer, end, playback, SMTP indirectly | Nothing else; the outer `try` in `handleCall` catches `Exception`. |
| `CallAudioController` | Every mute, route, mode, and root operation | Nothing else. |
| `RootShell` | Every process execution and timeout | Nothing else. |
| `EmailNotifier` | Every exception in the send coroutine | Nothing else. |
| `EmailSender` | `close` swallowing `IOException` | Every other failure raises `IOException` to the caller. |

## Idempotency and exactly-once effects

- A single notification per call is enforced by `isHandled` (dialer) and `isNotified` (legacy), both checked under a lock or on a single thread.
- `finishSession` and `endSession` are guarded so that the cleanup path runs once.
- `MediaPlayer` release is guarded by nulling the field after release.

## Retry policy

There is none. SMTP, root commands, file imports, role requests, and platform call operations are attempted once. A failed notification is lost.

## Recovery

| Failure | Recovery |
|---------|----------|
| Process killed during a call | No recovery; the call state and session are lost. |
| Playback failure | The sequence continues to disconnect and notify. |
| SMTP failure | The user resends manually or relies on the next call. |
| Root revoked | The cached state stays stale until `requestAccess` or the next process start. |
| Malformed settings JSON | The application cannot parse settings; the exception is unhandled. Clearing application data restores defaults. |

## Related documents

- [Logging](./logging.md)
- [Invariants](./invariants.md)
- [Troubleshooting](./troubleshooting.md)
- [Ambiguities and open questions](./ambiguities-and-open-questions.md)
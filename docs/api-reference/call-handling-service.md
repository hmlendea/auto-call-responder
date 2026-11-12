# CallHandlingService

Foreground service declared with `android:foregroundServiceType="phoneCall"` and `android:exported="false"`.

Implementation: `ro.nucilandia.autocallresponder.CallHandlingService`, file `CallHandlingService.kt`.

## Actions

| Action | Constant | Effect |
|--------|----------|--------|
| Start handling | `ACTION_START` (`"ACTION_START"`) | Show the foreground notification and run the handling coroutine. |
| Stop handling | `ACTION_STOP` (`"ACTION_STOP"`) | Cancel the sequence, stop audio, notify once, stop the service. |

## Extras

| Extra | Constant | Type | Consumer |
|-------|----------|------|----------|
| Caller number | `EXTRA_CALLER_NUMBER` (`"caller_number"`) | `String` | Both actions. |
| Pre-answer delay | `EXTRA_DELAY_SECONDS` (`"delay_seconds"`) | `Int` (default 0) | `ACTION_START` only; not set by the receiver. |

## Constants

| Constant | Value |
|----------|-------|
| `CHANNEL_ID` | `CallHandlingChannel` |
| `NOTIFICATION_ID` | `1001` |
| `MILLIS_PER_SECOND` | `1000` |
| `ANSWER_SETTLE_MILLISECONDS` | `1500` |
| `AUDIO_END_DELAY_MILLISECONDS` | `1000` |
| `AUDIO_PLAYBACK_TIMEOUT_SECONDS` | `120` |

## onStartCommand(intent, flags, startId)

Returns `START_NOT_STICKY`.

### ACTION_START

1. `startForeground(1001, createNotification(callerNumber))` on channel `CallHandlingChannel` with importance LOW.
2. `handleCall(callerNumber, delaySeconds)`.

### ACTION_STOP

`endSession(callerNumber)`.

## handleCall(callerNumber, delaySeconds)

Runs on `CoroutineScope(Dispatchers.IO)`.

| Step | Effect |
|------|--------|
| 1 | Create the session; store it in the `@Volatile activeSession`. |
| 2 | When `delaySeconds > 0`, `delay(delaySeconds * 1000)`. |
| 3 | `answerCall()` through `TelecomManager.acceptRingingCall`; record `answerTimeMillis`. |
| 4 | `controller.muteMicrophone()`. |
| 5 | `delay(1500 + internalDelay * 1000)`. |
| 6 | `playAudioFileAndAwait(controller)`. |
| 7 | `delay(1000)`; `endCall()` through `TelecomManager.endCall`. |
| 8 | `finally`: `restoreAfterPlayback`, `notifyCallHandled`, `stopSelf`. |

`CancellationException` is rethrown (logged before rethrow); any other `Exception` is logged.

## endSession(callerNumber)

| Branch | Behaviour |
|--------|-----------|
| Session present | Cancel the job, stop audio, clear the session, notify once, `stopSelf`. |
| No session | `startForeground` again, then report the ended call when the number is non-empty, otherwise `stopSelf`. |

## Outbound operations

| Operation | Guard |
|-----------|-------|
| `TelecomManager.acceptRingingCall` | `ANSWER_PHONE_CALLS` granted. |
| `TelecomManager.endCall` | API 28 or later and `ANSWER_PHONE_CALLS` granted. |
| Volume raise | `AudioManager.STREAM_VOICE_CALL` and `AudioManager.STREAM_MUSIC` to their maximum. |
| Playback | `MediaPlayer` with `USAGE_VOICE_COMMUNICATION` and `CONTENT_TYPE_SPEECH`. |

## Lifecycle

| Callback | Behaviour |
|----------|-----------|
| `onCreate` | Create the notification channel. |
| `onDestroy` | Stop audio and cancel the job. |
| `onBind` | Return `null`. |

## Guarantees

| Guarantee | Enforced by |
|-----------|-------------|
| One notification per call | `isNotified` under `@Synchronized`. |
| Playback cannot hang forever | `withTimeoutOrNull(120_000)`. |
| An interrupted run is not restarted | `START_NOT_STICKY`. |

## Error scenarios

| Scenario | Behaviour |
|----------|-----------|
| `ANSWER_PHONE_CALLS` missing | The answer is skipped and logged; the sequence continues to end, notify, and stop. |
| API below 28 | The disconnect is skipped and logged. |
| No audio configured | Playback returns immediately; the sequence continues. |
| Playback error or timeout | The wait completes; the sequence continues. |

## Flow references

- [Flow: legacy call handling](../flows/legacy-call-handling.md)
- [Call orchestration component](../components/call-orchestration.md)
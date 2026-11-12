# Call orchestration component

## Components

| Class | File | Role |
|-------|------|------|
| `AutoCallInCallService` | `AutoCallInCallService.kt` | Dialer-path sequence and per-call session. |
| `CallHandlingService` | `CallHandlingService.kt` | Legacy foreground-service sequence. |
| `RomanianPhoneNumbers` | `RomanianPhoneNumbers.kt` | Eligibility filter. |

## `AutoCallInCallService`

### `CallSession`

The session object owns the call, the normalised caller number, the identifier `"${handle}:${creationTimeMillis}"`, the ring time, the answered time, the `isEnding`, `isFinished`, and `isHandled` flags, and the list of scheduled tasks.

`schedule(delayMillis, action)` refuses to schedule while ending, wraps the action in a re-checking `Runnable`, records it, and posts it.

`cancelScheduledTasks` removes every recorded runnable from the handler.

### Sequence

| Step | Timing | Action |
|------|--------|--------|
| 1 | on `STATE_RINGING` | Create the session and schedule the answer. |
| 2 | after `delaySeconds` | Create a `CallAudioController`, mute, `call.answer(STATE_AUDIO_ONLY)`, record `answeredAtMillis`. |
| 3 | 600 ms after the answer | Re-apply the mute as a safety net. |
| 4 | 1500 ms + `internalDelaySeconds` after the answer | `prepareForPlayback(earpieceOnly)` then `playAudio`. |
| 5 | immediately before playback | Resolve `audioFilePath` then `audio_file_uri`; release any previous player; set attributes; prepare; unmute; start. |
| 6 | on completion or error | Mute and schedule `endCall` after 1000 ms. |
| 7 | at `endCall` | Mute, `stopAudio`, `call.disconnect`, `notifyCallHandled`. |
| 8 | on `STATE_DISCONNECTED` / `onCallRemoved` | `finishSession`: cancel tasks, stop audio, remove the session, notify once. |

### `stopAudio`

Stops and releases the player, then calls `audioController.restoreAfterPlayback()` and nulls the controller. Because the controller is nulled, a repeated `stopAudio` is a no-op.

### `notifyCallHandled`

Guarded by `isHandled`; calls `EmailNotifier.sendCallNotification` with the session's caller number, ring time, answered time, and `System.currentTimeMillis()`.

## `CallHandlingService`

A foreground service with `foregroundServiceType="phoneCall"` that runs the sequence on `Dispatchers.IO`.

### Constants

| Constant | Value |
|----------|-------|
| `CHANNEL_ID` | `CallHandlingChannel` |
| `NOTIFICATION_ID` | `1001` |
| `ACTION_START` | `ACTION_START` |
| `ACTION_STOP` | `ACTION_STOP` |
| `EXTRA_CALLER_NUMBER` | `caller_number` |
| `EXTRA_DELAY_SECONDS` | `delay_seconds` (unused by the receiver) |
| `ANSWER_SETTLE_MILLISECONDS` | `1500` |
| `AUDIO_END_DELAY_MILLISECONDS` | `1000` |
| `AUDIO_PLAYBACK_TIMEOUT_SECONDS` | `120` |

### `ACTION_START`

Calls `startForeground` with a low-importance notification showing the caller, then `handleCall(callerNumber, delaySeconds)`.

`handleCall` creates the session, launches the coroutine, optionally delays, answers through `TelecomManager.acceptRingingCall`, mutes, delays, plays and awaits, delays, ends, and in `finally` restores audio, notifies once, and stops the service.

### `ACTION_STOP`

`endSession`:

- when `activeSession` is null, starts the foreground notification and reports the ended call (or stops when the number is empty);
- otherwise cancels the job, stops audio, clears the session, notifies once, and stops the service.

### Playback

`playAudioFileAndAwait` resolves the same audio precedence, calls `prepareForPlayback`, creates a `MediaPlayer` with `USAGE_VOICE_COMMUNICATION` and `CONTENT_TYPE_SPEECH`, completes a `CompletableDeferred` on completion or error, unmutes, starts, raises both `STREAM_VOICE_CALL` and `STREAM_MUSIC` to maximum volume, and awaits with a 120-second timeout before muting and stopping.

### Service lifecycle

`onStartCommand` returns `START_NOT_STICKY`. `onDestroy` stops audio and cancels the job. `onBind` returns `null`.

## `RomanianPhoneNumbers`

| Member | Behaviour |
|--------|-----------|
| `isRomanian(rawNumber)` | Normalise, convert to national form, and match `^0[237]\d{8}$`. |
| `normalize` | Remove a `tel:` prefix, strip whitespace, hyphens, dots, parentheses, and slashes, then trim. |
| `toNationalForm` | Remove `+40`, `0040`, or bare `40` and prepend `0`; otherwise return the number unchanged. |

The matcher has no Android dependency and is the only unit-tested component.

## Related documents

- [Flows: dialer call handling](../flows/dialer-call-handling.md)
- [Flows: legacy call handling](../flows/legacy-call-handling.md)
- [Behaviour: audio playback and mute](../behaviour/audio-playback-and-mute.md)
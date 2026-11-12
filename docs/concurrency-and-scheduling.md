# Concurrency and scheduling

## Execution contexts

| Context | Used by | Notes |
|---------|---------|-------|
| Main looper | `AutoCallInCallService` (all sequence work), `PhoneStateReceiver` (`Handler`), `MainActivity` UI, `RootShell` callbacks | The in-call path deliberately schedules everything on the main looper so cancellation and state checks are serialised. |
| `Dispatchers.IO` coroutine | `CallHandlingService` (call sequence), `EmailNotifier` (SMTP), `MainActivity` (test e-mail) | Cancellable work that performs network or blocking operations. |
| Dedicated thread | `RootShell` detection (`RootShell-Detect`) | Avoids blocking any caller while the root manager shows a prompt. |
| SMTP socket thread | `EmailSender` (runs on the caller's IO context) | Blocking socket I/O with 20-second connect and read timeouts. |

## Dialer-path scheduling

`AutoCallInCallService` keeps a `CallSession` per call. Every delay is scheduled through `CallSession.schedule`, which:

1. returns immediately when `isEnding` is true;
2. wraps the action in a `Runnable` that re-checks `isEnding`;
3. records the runnable in `scheduledTasks`;
4. posts it with `Handler.postDelayed`.

`finishSession` sets `isFinished` and `isEnding`, cancels every recorded runnable, stops playback, removes the session, and sends the single notification.

| Scheduled action | Delay | Purpose |
|------------------|-------|---------|
| `answer(session)` | `delaySeconds * 1000` | Answer after the configured ringing delay. |
| `controller.muteMicrophone()` | 600 ms | Safety net if the `STATE_ACTIVE` callback is lost. |
| `prepareForPlayback` + `playAudio` | 1500 ms + `internalDelaySeconds * 1000` | Let the call settle, then start audio. |
| `endCall(session)` | 1000 ms after completion or error | Brief pause, then disconnect. |

## Legacy-path scheduling

`CallHandlingService.handleCall` runs the sequence inside `scope.launch` on `Dispatchers.IO` and stores the `Job` in `callJob`:

- `delay(delaySeconds * 1000)` when the caller passes a delay (the receiver does not pass one);
- `answerCall()`, `controller.muteMicrophone()`;
- `delay(1500 + internalDelay * 1000)`;
- `playAudioFileAndAwait`, which suspends on a `CompletableDeferred` completed by `onCompletion` or `onError` and is bounded by `withTimeoutOrNull(120_000)`;
- `delay(1000)`, then `endCall()`;
- `finally` restores audio, notifies once, and calls `stopSelf()`.

`ACTION_STOP` calls `endSession`, which cancels `callJob`, stops audio, clears `activeSession`, notifies once, and stops the service. A second `ACTION_STOP` for the same call is harmless because the session is already cleared and `isNotified` guards the message.

`PhoneStateReceiver` uses a main-looper `Handler` to delay `startForegroundService(ACTION_START)` and calls `handler.removeCallbacksAndMessages(null)` when the call becomes idle.

## State visibility

| Field | Declaration | Reason |
|-------|-------------|--------|
| `CallHandlingService.activeSession` | `@Volatile` | Written by the sequence coroutine and read by the main-thread `ACTION_STOP` path. |
| `CallHandlingService.notifyCallHandled` | `@Synchronized` | Prevents two callers from emitting the message. |
| `CallHandlingService.stopAudio` | `@Synchronized` | Prevents concurrent release of the player. |
| `PhoneStateReceiver.lastState` | `@Volatile` static | Deduplicates consecutive identical broadcasts across receiver instances. |
| `RootShell.availability` | `@Volatile` | Read by many threads; written by the detection thread. |
| `RootShell.detectionInProgress` | `AtomicBoolean` | Ensures one detection at a time. |

## Concurrency limits

- Exactly one call is modelled. `AutoCallInCallService` holds one `mediaPlayer` and one `audioController`; `CallHandlingService` holds one `activeSession`.
- There is no queue, worker pool, lock hierarchy, or backpressure mechanism.
- The SMTP transaction is not cancelled when the call handler is destroyed; it runs to completion or timeout.
- Root detection is not retried automatically after it completes; it is re-run only by `requestAccess` or the next boot.

## Related documents

- [Flows: dialer call handling](./flows/dialer-call-handling.md)
- [Flows: legacy call handling](./flows/legacy-call-handling.md)
- [Invariants](./invariants.md)
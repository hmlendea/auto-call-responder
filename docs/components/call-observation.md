# Call observation component

## Components

| Class | File | Trigger |
|-------|------|---------|
| `CallScreeningService` | `CallScreeningService.kt` | Telecom screening request (Android 10+, call-screening role). |
| `AutoCallInCallService` | `AutoCallInCallService.kt` | Telecom in-call binding (dialer role). |
| `PhoneStateReceiver` | `PhoneStateReceiver.kt` | `android.intent.action.PHONE_STATE`. |
| `BootReceiver` | `BootReceiver.kt` | `BOOT_COMPLETED`, `QUICKBOOT_POWERON`, `MY_PACKAGE_REPLACED`. |

## `CallScreeningService`

`onScreenCall` performs four steps:

1. Extract the caller number: `callDetails.handle?.schemeSpecificPart`, with a `tel:` prefix removed and whitespace stripped.
2. Respond with `ALLOW_RESPONSE` (an empty `CallResponse`), which neither disallows, rejects, nor silences the call.
3. Read `SettingsManager.getSettings()` and return when `enabled` is false.
4. Return when `filterRomanianNumbers` is true and `RomanianPhoneNumbers.isRomanian(callerNumber)` is false.

The service never answers, plays, or stores anything. Its only durable effect is the allow response and the log line that records whether the call would be handled.

## `AutoCallInCallService`

The authoritative handler while the application holds the dialer role.

### State

- `sessions: MutableMap<String, CallSession>` keyed by `"${handle}:${creationTimeMillis}"`.
- `mediaPlayer: MediaPlayer?` and `audioController: CallAudioController?`, both single-call.
- `callCallback`, registered per call in `onCallAdded` and unregistered in `onCallRemoved`.

### Callback behaviour

| Callback | Behaviour |
|----------|-----------|
| `onCallAdded` | Register the callback; if the call is already `STATE_RINGING`, call `scheduleAnswer`. |
| `STATE_RINGING` | `scheduleAnswer`. |
| `STATE_ACTIVE` | Re-apply `audioController.muteMicrophone()`. |
| `STATE_DISCONNECTED` | `finishSession`. |
| `onCallRemoved` | Unregister, finish the session if present. |

### `scheduleAnswer`

Returns when the responder is disabled, when the number fails the active filter, or when a session already exists for the identifier. Otherwise it creates the session and schedules `answer` after `delaySeconds * 1000`.

### Sequence helpers

`answer`, `playAudio`, `endCall`, `finishSession`, `notifyCallHandled`, and `stopAudio` are documented in [Call orchestration](./call-orchestration.md#autocalincallservice).

## `PhoneStateReceiver`

### State

A private `incomingNumber` string and a static `@Volatile lastState` that deduplicates consecutive identical broadcasts.

### Behaviour

| State | Behaviour |
|-------|-----------|
| `CALL_STATE_RINGING` | Ignore when `isDefaultDialer(context)`. Otherwise normalise the number, check `enabled`, check the Romanian filter, and post `startCallHandling` after `delaySeconds * 1000` on the main-looper handler. |
| `CALL_STATE_OFFHOOK` | Log only. |
| `CALL_STATE_IDLE` | `handler.removeCallbacksAndMessages(null)`, call `stopCallHandling` with the recorded number, clear `incomingNumber`. |

`isDefaultDialer` returns false below Android 10 and otherwise consults `RoleManager.ROLE_DIALER`. `stopCallHandling` also returns early when the dialer role is held.

## `BootReceiver`

`onReceive` ignores actions other than the three boot-related ones, then:

1. `SettingsManager.init(context)`.
2. On Android 10+, logs whether the dialer and call-screening roles are held.
3. Logs whether battery optimisation is disabled.
4. When the responder is enabled, logs readiness and calls `RootShell.detectAsync()`.

It never starts a service; Telecom binds the services on its own when the roles survive the reboot.

## Manifest declarations

| Component | Exported | Permission | Intent filter |
|-----------|----------|------------|---------------|
| `CallScreeningService` | `true` | `android.permission.BIND_SCREENING_SERVICE` | `android.telecom.CallScreeningService` |
| `AutoCallInCallService` | `true` | `android.permission.BIND_INCALL_SERVICE` | `android.telecom.InCallService`; metadata `IN_CALL_SERVICE_UI=true` |
| `PhoneStateReceiver` | `true` | — | `android.intent.action.PHONE_STATE` |
| `BootReceiver` | `true` | — | `BOOT_COMPLETED`, `QUICKBOOT_POWERON`, `MY_PACKAGE_REPLACED` (priority 1000) |

## Related documents

- [Flow: dialer call handling](../flows/dialer-call-handling.md)
- [Flow: legacy call handling](../flows/legacy-call-handling.md)
- [Interface reference: in-call service](../api-reference/in-call-service.md)
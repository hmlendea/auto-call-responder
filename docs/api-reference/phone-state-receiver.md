# PhoneStateReceiver

Receiver for `android.intent.action.PHONE_STATE`, declared exported with no bind permission.

Implementation: `ro.nucilandia.autocallresponder.PhoneStateReceiver`, file `PhoneStateReceiver.kt`.

## onReceive(context: Context, intent: Intent)

### Request

| Extra | Type | Meaning |
|-------|------|---------|
| `TelephonyManager.EXTRA_STATE` | `Int` | `CALL_STATE_RINGING`, `CALL_STATE_OFFHOOK`, or `CALL_STATE_IDLE`. |
| `TelephonyManager.EXTRA_INCOMING_NUMBER` | `String?` | Caller number when the device permits it. |

The receiver returns immediately when the action is not `PHONE_STATE`. A state equal to the cached `lastState` is ignored.

### Behaviour by state

| State | Behaviour |
|-------|-----------|
| `CALL_STATE_RINGING` | Return when the dialer role is held. Otherwise normalise the number, return when `enabled` is false or the filter rejects the number, then post `startCallHandling` after `delaySeconds * 1000`. |
| `CALL_STATE_OFFHOOK` | Log only. |
| `CALL_STATE_IDLE` | Cancel every pending handler message, call `stopCallHandling` with the recorded number, clear the number. |

### Outbound effects

| Effect | Mechanism |
|--------|-----------|
| Start handling | `startForegroundService(Intent(context, CallHandlingService).putExtra(EXTRA_CALLER_NUMBER, number))` with action `ACTION_START`. |
| Stop handling | `startForegroundService(...)` with action `ACTION_STOP` and the number; exceptions are logged as warnings. |

### Role gating

`isDefaultDialer` returns false below Android 10 and otherwise returns `RoleManager.isRoleHeld(ROLE_DIALER)`. It gates the `RINGING` branch and `stopCallHandling`, so the legacy path never runs while the dialer path is authoritative.

### Deduplication

`lastState` is a static `@Volatile` field shared across receiver instances, so repeated identical broadcasts do not restart the sequence.

## Guarantees

| Guarantee | Enforced by |
|-----------|-------------|
| The legacy path never coexists with the dialer path | `isDefaultDialer` in both branches. |
| A call that ended during the delay still produces a notification | `stopCallHandling` forwards the number; `CallHandlingService` reports the ended call. |
| Pending third-party callbacks are cleared on idle | `handler.removeCallbacksAndMessages(null)`. |

## Error scenarios

| Scenario | Behaviour |
|----------|-----------|
| Caller number unavailable | Empty number; the filter rejects it when enabled. |
| `startForegroundService` for `ACTION_STOP` throws | Logged as a warning; the call is already over. |
| `startForegroundService` for `ACTION_START` throws | Not caught; the platform raises `ForegroundServiceStartNotAllowedException` in the background on newer versions. |

## Flow references

- [Flow: legacy call handling](../flows/legacy-call-handling.md)
- [Ambiguities and open questions](../ambiguities-and-open-questions.md)
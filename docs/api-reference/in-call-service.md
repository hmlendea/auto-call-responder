# InCallService

Base contract: `android.telecom.InCallService`, declared with the action `android.telecom.InCallService`, the permission `android.permission.BIND_INCALL_SERVICE`, and the metadata `android.telecom.IN_CALL_SERVICE_UI=true`.

Implementation: `ro.nucilandia.autocallresponder.AutoCallInCallService`, file `AutoCallInCallService.kt`.

Telecom binds the service for as long as the application holds the dialer role. This is the authoritative handling path.

## onCallAdded(call: Call)

### Request

| Input | Type | Notes |
|-------|------|-------|
| `call` | `android.telecom.Call` | The newly added call. |

### Behaviour

1. Register the shared `Call.Callback`.
2. When `call.state == STATE_RINGING`, call `scheduleAnswer`.

## Call.Callback.onStateChanged(call, state)

| State | Behaviour |
|-------|-----------|
| `STATE_RINGING` | `scheduleAnswer(call)`. |
| `STATE_ACTIVE` | Re-apply `audioController?.muteMicrophone()`. |
| `STATE_DISCONNECTED` | `finishSession(session)`. |
| Any other | Ignored. |

The callback returns immediately when no session exists for the call identifier.

## onCallRemoved(call: Call)

1. Unregister the callback.
2. If a session exists, `finishSession`.

## scheduleAnswer(call: Call)

### Eligibility

Returns without scheduling when any of the following holds:

- `settings.enabled` is false;
- `settings.filterRomanianNumbers` is true and the number is not Romanian;
- a session already exists for `"${handle}:${creationTimeMillis}"`.

Otherwise it creates a `CallSession` and schedules `answer` after `settings.delaySeconds * 1000`.

## Outbound calls used

| Call | Signature | Parameters | Errors |
|------|-----------|------------|--------|
| Answer | `Call.answer(videoState)` | `VideoProfile.STATE_AUDIO_ONLY` | Caught; the controller is unmuted and the sequence stops. |
| Disconnect | `Call.disconnect()` | — | Caught and logged; the notification is still sent. |
| Mute | `InCallService.setMuted(boolean)` | Via `CallAudioController` | Caught; falls back to root then `AudioManager`. |
| Route | `InCallService.setAudioRoute(int)` | `CallAudioState.ROUTE_EARPIECE` or `ROUTE_SPEAKER` | Caught; falls back to `AudioManager`. |

## Timing constants

| Constant | Value |
|----------|-------|
| `MILLIS_PER_SECOND` | `1000` |
| `ANSWER_SETTLE_MILLISECONDS` | `1500` |
| `AUDIO_END_DELAY_MILLISECONDS` | `1000` |
| `MICROPHONE_MUTE_REAPPLY_MILLISECONDS` | `600` |

## Guarantees

| Guarantee | Enforced by |
|-----------|-------------|
| One answer per call | Session map keyed by handle + creation time. |
| One notification per call | `isHandled`. |
| Cancelled delays on disconnect | `isEnding` plus `cancelScheduledTasks`. |
| The microphone is muted outside playback | Mute before the answer, on `STATE_ACTIVE`, and after playback. |

## Error scenarios

| Scenario | Behaviour |
|----------|-----------|
| `call.answer` throws | `unmuteMicrophone`, clear the controller, stop. The session remains and notifies on disconnect. |
| No audio configured | End the call. |
| Player prepare or start throws | Mute and end the call. |
| Playback error | Treated as completion: mute, delay, end. |

## Flow references

- [Flow: dialer call handling](../flows/dialer-call-handling.md)
- [Behaviour: audio playback and mute](../behaviour/audio-playback-and-mute.md)
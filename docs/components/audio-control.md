# Audio control component

## Class

`internal class CallAudioController(context: Context, inCallService: InCallService? = null)` in `CallAudioController.kt`.

An instance is created per handled call and discarded when the call ends. The dialer path passes `this` as the `InCallService`; the legacy path passes only the context.

## State

| Property | Meaning |
|----------|---------|
| `isMicrophoneMuted` | Last mute value applied by this instance. |
| `isUplinkRoutingActive` | Whether at least one root uplink route control was applied. |
| `isRootAvailable` | Whether root was detected during this call. |

## Operations

### `muteMicrophone` and `unmuteMicrophone`

`applyMicrophoneMute(muted)` tries, in order:

1. `InCallService.setMuted(muted)` when a service was supplied;
2. `RootShell.execute("cmd audio set-mic-mute $muted")` when root is available;
3. `AudioManager.isMicrophoneMute = muted`.

Each attempt is wrapped and logged. Failure of the first two falls through to the next; failure of the third returns `false`.

### `prepareForPlayback(earpieceOnly)`

1. `setAudioRoute(earpieceOnly)`.
2. `AudioManager.mode = MODE_IN_COMMUNICATION`.
3. `applyRootUplinkRouting()`.

`setAudioRoute` uses `InCallService.setAudioRoute(ROUTE_EARPIECE)` or `ROUTE_SPEAKER`, falling back to `AudioManager.isSpeakerphoneOn = !earpieceOnly`.

### `restoreAfterPlayback`

1. `restoreRootUplinkRouting()`.
2. `AudioManager.mode = MODE_NORMAL`.

### Root uplink routing

`applyRootUplinkRouting`:

1. Sets `isRootAvailable = RootShell.isAvailable()`; returns when false.
2. Finds the first mixer binary from `tinymix`, `tiny-mix`, `/vendor/bin/tinymix` by running `command -v <candidate>` and checking the output contains the candidate.
3. Runs each `(control, value)` pair in `UPLINK_ROUTES` and counts non-null results.
4. Sets `isUplinkRoutingActive` when at least one succeeded.

`restoreRootUplinkRouting` runs the mirror entries of `UPLINK_RESTORE_ROUTES` when routing was active, then clears the flag.

## Uplink catalogue

| Control | On | Off |
|---------|----|-----|
| `Voice Tx Mixer MultiMedia1` | `1` | `0` |
| `Voice Tx Mixer MultiMedia2` | `1` | `0` |
| `Incall_Music Audio Mixer MultiMedia1` | `1` | `0` |
| `Incall_Music Audio Mixer MultiMedia2` | `1` | `0` |
| `SEC_AUDIO_LOOPBACK` | `1` | `0` |

The catalogue covers common Qualcomm and MediaTek mixer names. It is best-effort: a device whose kernel exposes no such control simply keeps local playback.

## Degradation

| Missing capability | Result |
|--------------------|--------|
| `InCallService` | Mute and route fall back to root then `AudioManager`. |
| Root | Local playback only; mute still attempts `AudioManager`. |
| Mixer binary | Local playback only. |
| Matching mixer control | Local playback only. |

## Related documents

- [Root integration](./root-integration.md)
- [Behaviour: audio playback and mute](../behaviour/audio-playback-and-mute.md)
- [Design decisions](../design-decisions.md)
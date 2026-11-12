# Behaviour: audio playback and mute

## Playback

| Aspect | Behaviour |
|--------|-----------|
| Source | `AppSettings.audioFilePath` when non-blank, otherwise the `audio_file_uri` preference. |
| URI | A `content://` URI resolved through the `FileProvider` authority `${applicationId}.fileprovider`. |
| Attributes | `USAGE_VOICE_COMMUNICATION` and `CONTENT_TYPE_SPEECH`. |
| Start | After the call settles and the configured internal delay elapses. |
| End detection | `onCompletion` or `onError`; both release the wait on the legacy path and schedule the end on the dialer path. |
| Timeout | 120 seconds on the legacy path only. |
| Volume | Raised to maximum for `STREAM_VOICE_CALL` and `STREAM_MUSIC` on the legacy path. |

## Route selection

`earpieceOnly` (default true) selects the earpiece; false selects the loudspeaker. The dialer path uses `InCallService.setAudioRoute` with `ROUTE_EARPIECE` or `ROUTE_SPEAKER`; the legacy path falls back to `AudioManager.isSpeakerphoneOn`. The audio mode is set to `MODE_IN_COMMUNICATION` before playback and restored to `MODE_NORMAL` afterwards.

## Microphone contract

The microphone follows a strict contract:

| Phase | State | Reason |
|-------|-------|--------|
| Before the answer | Muted | Ensure the caller never hears the room from the first instant. |
| On `STATE_ACTIVE` | Muted again | Telecom resets the mute flag during the transition. |
| Immediately before `start` | Unmuted | The recording is audible to the caller only if the uplink is open (root routing). |
| On completion, error, or early end | Muted | Ambient sound must never reach the caller. |
| After the call | Muted | The controller is discarded with the mute applied. |

The 600 ms re-apply task on the dialer path is a safety net when the `STATE_ACTIVE` callback is lost or reordered.

## Uplink routing (root)

When root is available and a mixer binary exposes a matching control, the controller applies the uplink loopback pairs so the caller can hear the recording. When root or the control is absent, playback remains local to the device and the caller hears nothing. The applied controls are restored when the call ends.

## Degradation

| Condition | Observable behaviour |
|-----------|----------------------|
| No audio configured | The call is still answered, muted, and ended; the notification is sent with "No audio file configured". |
| Unreadable audio file | Playback fails, the failure is logged, and the call is still ended and notified. |
| In-call mute API unavailable | The controller tries root, then `AudioManager`. |
| Route API unavailable | The controller falls back to `AudioManager`. |
| Root unavailable | Local playback only. |

## Related documents

- [Audio control component](../components/audio-control.md)
- [Behaviour: automatic call answering](./automatic-call-answering.md)
- [Invariants](../invariants.md)
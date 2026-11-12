# Ambiguities and open questions

This document records behaviour that is unclear, inconsistent, or dead in the current implementation. Each item is derived from the source; none of these are external requirements.

## Dead or ineffective parameters

| Item | Evidence | Impact |
|------|----------|--------|
| `CallHandlingService.EXTRA_DELAY_SECONDS` is never set | `PhoneStateReceiver.startCallHandling` sets only `EXTRA_CALLER_NUMBER`; the initial `handler.postDelayed` performs the pre-answer delay | The service's internal `delay(delaySeconds * 1000)` is always zero. The delay is implemented in the receiver. If the receiver's delay were removed, the answer would occur immediately. |
| `AppLifecycleObserver` callbacks are empty | `AutoCallResponderApplication.kt` | The observer is registered but does nothing; it exists as a placeholder. |
| `lifecycle-viewmodel-ktx` and `preference-ktx` are declared | `app/build.gradle.kts` | No `ViewModel` or preference object is used. |
| `MainActivity.etDelay` is hidden | `activity_settings.xml` sets `android:visibility="gone"` | It exists only as the value holder synchronised by the seek bar. |

## Inconsistent behaviour

| Item | Evidence | Impact |
|------|----------|--------|
| The dialer role request uses the call-screening request code | `MainActivity.requestDialerRole` calls `startActivityForResult(intent, REQUEST_CODE_CALL_SCREENING)` and `onActivityResult` handles only `AudioFilePicker.REQUEST_CODE` | The result code is not consumed, so the mismatch is currently harmless but confusing. |
| `requestPermissions` requests the dialer role **or** the call-screening role, not both | The `if/else if` in `requestPermissions` | The user may end up with the dialer role but no call-screening role, so calls are not observed even though they can be answered. |
| The dialer role request is not repeated after the screening role is granted | Same `if/else if` | Re-entering the screen and tapping again is required to obtain the second role. |
| `isDefaultDialer` guards `stopCallHandling` | `PhoneStateReceiver.stopCallHandling` returns early when the dialer role is held | On the dialer path the foreground service is never started, so no stop is needed; the early return is defensive. |
| Root detection is not invalidated on revocation | `RootShell.requestAccess` is the only reset | A revoked root grant leaves a stale `true` until the next detection or process restart. |
| `SettingsManager` does not observe external preference changes | No `SharedPreferences.OnSharedPreferenceChangeListener` is registered | A change made outside the process is picked up only after the process restarts. |

## Unverified behaviour

| Item | Why it is unverified |
|------|----------------------|
| Uplink routing success on a specific device | Depends on the kernel mixer; the catalogue is best-effort. |
| `cmd audio set-mic-mute` availability | Depends on the device shell implementation. |
| Answer and disconnect on every vendor firmware | Depends on the Telecom implementation of the vendor. |
| Caller number availability | Depends on the carrier and the device caller-identifier policy. |
| Playback codec support | Depends on the device `MediaPlayer` codecs. |

## Known gaps

- No automated test covers the call sequence, the audio controller, the settings store, the SMTP client, the file provider, or the boot receiver.
- No structured logging, metrics, or trace exists.
- No retry or durable outbox exists for notifications.
- No migration framework exists for the settings schema.
- Concurrent calls are not modelled.
- No cancellation exists for an in-flight SMTP transaction when the handler is destroyed.

## Questions a future maintainer may need to answer

1. Should `EXTRA_DELAY_SECONDS` be removed, or should the receiver stop performing the delay?
2. Should the dialer and call-screening roles always be requested together?
3. Should the imported audio file be included in cloud backup alongside the settings?
4. Should the settings schema gain an explicit version for safe migrations?
5. Should a second simultaneous call be rejected explicitly rather than implicitly unmodelled?

## Related documents

- [Design decisions](./design-decisions.md)
- [Error handling](./error-handling.md)
- [Invariants](./invariants.md)
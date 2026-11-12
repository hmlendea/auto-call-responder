# Frequently asked questions

## Does the application send audio to the caller?

No. Public Android APIs cannot inject audio into the voice call uplink. The recording plays on the device audio route. Root plus a mixer control that the device kernel exposes is the only route that can attempt uplink routing, and it is attempted opportunistically.

## Why are incoming calls sometimes not observed?

Call screening requires the call-screening role on Android 10 and later. Without the role, `CallScreeningService` is not bound. On Android 8 and 9, calls are observed through the `PHONE_STATE` broadcast instead.

## Why does the application need the dialer role?

The dialer role is what makes Telecom bind `AutoCallInCallService`, which is the only supported way to set the microphone mute and the call audio route, and to answer and disconnect a specific `Call`. The legacy path can answer and end calls through `TelecomManager`, but its mute and route controls are best-effort.

## Why are two call paths implemented?

Android 8 and 9 do not expose the role-bound call-screening services in the same way, and the dialer role is not held in every installation. The legacy receiver keeps the application usable in both situations. The legacy path is deliberately suppressed while the dialer role is held.

## Is the audio file copied or referenced?

Copied. The selected document is read once and written to `filesDir/call_audio/selected_call_audio.<ext>`. On each import the directory is purged, so only one file is retained.

## Where is the SMTP password stored?

In the private shared preferences file `AutoCallResponderPrefs`, unencrypted, inside the application sandbox. It is included in cloud backup because `backup_rules.xml` and `data_extraction_rules.xml` include that file.

## Is the notification sent if the call is never answered?

On the dialer path, a call that ends before the answer is handled by `finishSession`, which still calls `notifyCallHandled` with `answerTimeMillis` zero; the body reports "not answered automatically". On the legacy path, `PhoneStateReceiver` forwards the number through `ACTION_STOP` so the service reports it.

## Why does the call end one second after the recording?

`AUDIO_END_DELAY_MILLISECONDS` is `1000` on both paths. It gives the platform a brief pause between the end of playback and the disconnect.

## What happens if playback fails?

The completion and error listeners both release the wait, so the sequence proceeds: mute, delay, disconnect, notify. The failure is logged.

## Can the application handle two calls at once?

No. Both paths model a single call. The dialer path keeps one player and one audio controller; the legacy path keeps one active session.

## Why is the caller number sometimes empty?

Some devices restrict caller identification. An empty number fails the Romanian filter, so such calls are not handled while the filter is enabled.

## Does the application retry a failed e-mail?

No. Delivery is asynchronous and fire-and-forget, with no retry, queue, or durable outbox.

## Why is battery optimisation relevant?

Aggressive optimisation can delay the scheduled answer. The application can only request an exemption; it cannot grant it.

## Does the repository contain a server or backend?

No. The application is entirely device-local. The only outbound network traffic is SMTP.

## Related documents

- [Repository overview](./repository-overview.md)
- [Troubleshooting](./troubleshooting.md)
- [Ambiguities and open questions](./ambiguities-and-open-questions.md)
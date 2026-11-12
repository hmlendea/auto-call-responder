# Behaviour: automatic call answering

## Overview

The application answers eligible incoming calls automatically after a configurable delay, plays a recording, mutes the microphone outside the recording, ends the call after playback, and notifies the owner by e-mail. This document describes the user-observable behaviour and the rules that produce it.

## Eligibility rules

A call is handled when **all** of the following are true:

1. `AppSettings.enabled` is true.
2. Either `filterRomanianNumbers` is false, or `RomanianPhoneNumbers.isRomanian(callerNumber)` is true.
3. The relevant path can act: the dialer role is held (dialer path), or the dialer role is not held and the legacy receiver can start the service.

The filter is evaluated before any answering. A caller whose identifier is unavailable produces an empty number, which fails the filter while it is enabled.

## Romanian number grammar

| Form | Examples |
|------|----------|
| National | `0212345678`, `0312345678`, `0722512537` |
| International with `+` | `+40212345678`, `+40722512537` |
| International dialling | `0040212345678`, `0040722512537` |
| Bare country code | `40212345678` |
| Formatted | `0722 512 537`, `+40 (722) 512-537`, `tel:+40376509045` |

Accepted numbers must match `^0[237]\d{8}$` after normalisation, so the leading national digit must be `2`, `3`, or `7` and exactly eight further digits must follow.

## Timing

| Event | Timing |
|-------|--------|
| Call answered | `delaySeconds` after the call starts ringing (1–60 s). |
| Audio starts | `1500 ms` + `internalDelaySeconds` after the answer (0–60 s). |
| Audio stops | On completion or error. |
| Call ends | `1000 ms` after the audio stops. |
| Notification | Immediately after the disconnect attempt. |

## Ending behaviour

The call ends when the recording finishes, whether it completed normally or failed. If the caller hangs up first, the pending delays are cancelled and the audio is stopped; the notification is still sent.

## Idempotency

- A repeated ringing event for the same call does not schedule a second answer.
- Exactly one e-mail is sent per call, whether the application or the caller ended it.

## What is not done

- The call is never audibly injected into the caller's audio path through public APIs.
- The microphone is never left unmuted after playback.
- The application never answers a call while it is disabled.

## Related documents

- [Behaviour: audio playback and mute](./audio-playback-and-mute.md)
- [Behaviour: call notification e-mails](./call-notification-emails.md)
- [Configuration](../configuration.md)
- [Invariants](../invariants.md)
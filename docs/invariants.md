# Invariants

These statements must remain true across every change. Each is paired with where it is enforced and how a change would break it.

## Call-eligibility invariants

| Invariant | Enforced by | Breaking change |
|-----------|-------------|-----------------|
| A call is handled only when `AppSettings.enabled` is true | `CallScreeningService.onScreenCall`, `AutoCallInCallService.scheduleAnswer`, `PhoneStateReceiver.onReceive` | Removing any check makes the responder act while disabled. |
| The Romanian filter applies only when `filterRomanianNumbers` is true | `settings.filterRomanianNumbers && !RomanianPhoneNumbers.isRomanian(...)` in both paths | Inverting or dropping the conjunction changes eligibility. |
| Both paths apply the same eligibility decision | Duplicated checks in observation and orchestration | Editing one path only produces inconsistent behaviour. |
| The legacy broadcast path is suppressed while `ROLE_DIALER` is held | `PhoneStateReceiver.isDefaultDialer` guards on `RINGING` and `STOP` | Removing the guard makes both paths answer the same call. |
| A repeated `STATE_RINGING` does not schedule a second answer | `sessions.containsKey(identifier)` with identifier `handle:creationTimeMillis` | Removing the session map check double-answers. |

## Answering-sequence invariants

| Invariant | Enforced by | Breaking change |
|-----------|-------------|-----------------|
| The microphone is muted before and after the answer transition | `muteMicrophone()` before `call.answer` and on `STATE_ACTIVE` | The caller may hear room audio during the transition. |
| The microphone is unmuted only while the recording plays | `unmuteMicrophone()` immediately before `start`, `muteMicrophone()` on completion/error/end | Ambient sound reaches the caller. |
| The answer delay is measured from the ringing state | `session.schedule(settings.delaySeconds * 1000)` and `handler.postDelayed` | The configured delay no longer matches observable behaviour. |
| Playback starts after `ANSWER_SETTLE_MILLISECONDS` plus `internalDelaySeconds` | `session.schedule(1_500 + internalDelay * 1000)` and `delay(1_500 + internalDelay * 1000)` | The audio starts while the call is still settling or too early/late. |
| The call is disconnected after `AUDIO_END_DELAY_MILLISECONDS` | `session.schedule(1_000) { endCall }`; `delay(1_000); endCall()` | The call ends while audio is still playing or immediately after. |
| A call that ends early cancels every pending delay and stops playback | `finishSession` / `endSession` cancel tasks and stop audio | A delayed action later answers or disconnects an unrelated call. |
| Every scheduled action re-checks the ending flag | `CallSession.schedule` wraps the action | A race can run an action after the call ended. |

## Notification invariants

| Invariant | Enforced by | Breaking change |
|-----------|-------------|-----------------|
| Exactly one notification per call | `isHandled` (dialer) and `isNotified` (legacy) flags | Duplicate e-mails. |
| The notification is dispatched only after the disconnect attempt | `notifyCallHandled` follows `disconnect`/`endCall` | The end timestamp is wrong and SMTP delays the end. |
| A call that ended during the pre-answer delay still notifies on the legacy path | `PhoneStateReceiver.stopCallHandling` forwards the number; `CallHandlingService.endSession` reports it | The owner receives no notification. |
| Notification is suppressed when the recipient, user, or password is blank | `EmailNotifier.send` | An INVALID SMTP session is attempted with incomplete credentials. |
| Notification failure never propagates to the caller flow | Exceptions are caught inside the coroutine | A network failure could crash or delay handling. |

## Storage invariants

| Invariant | Enforced by | Breaking change |
|-----------|-------------|-----------------|
| `SettingsManager` is the only reader and writer of shared preferences | No other class calls `getSharedPreferences` for `AutoCallResponderPrefs` | Divergent or lost state. |
| The settings JSON is written under key `settings` in `AutoCallResponderPrefs` | `SettingsManager` constants | Existing installations lose their configuration. |
| The imported audio directory contains at most one file | `AudioFilePicker.createTargetFile` purges the directory | Unbounded storage growth. |
| The file provider authority is `${applicationId}.fileprovider` | Manifest provider declaration and `AudioFilePicker.getUriForFile` | Playback cannot resolve the URI. |
| `audioFilePath` wins over `audio_file_uri` when non-blank | Both playback sites | The wrong recording plays. |

## Security invariants

| Invariant | Enforced by | Breaking change |
|-----------|-------------|-----------------|
| Passwords are never logged | No log statement includes `smtpPassword` | Credential leakage to logcat. |
| Root commands are bounded by a timeout | `RootShell.execute` uses a five-second `waitFor` | A hung `su` command blocks handling. |
| Root detection never blocks the main thread | `RootShell.detectAsync` runs on a dedicated thread | The UI or the answer sequence freezes behind a root prompt. |
| Exported components require their platform permissions | Manifest declares `BIND_SCREENING_SERVICE` and `BIND_INCALL_SERVICE` | Other applications could bind the services. |

## Related documents

- [Change guide](./change-guide.md)
- [Design decisions](./design-decisions.md)
- [Data model](./data-model.md)
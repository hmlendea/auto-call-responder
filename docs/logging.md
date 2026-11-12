# Logging

## Framework

The application uses the Android platform logger, `android.util.Log`. No logging library, structured logging, file sink, or remote sink is configured.

## Tags

| Tag | Emitting component |
|-----|--------------------|
| `AutoCallResponderApplication` | (none; the class logs nothing) |
| `MainActivity` | `MainActivity` |
| `SettingsActivity` | `SettingsActivity` |
| `AudioFilePicker` | `AudioFilePicker` |
| `CallScreeningService` | `CallScreeningService` |
| `AutoCallInCallService` | `AutoCallInCallService` |
| `PhoneStateReceiver` | `PhoneStateReceiver` |
| `BootReceiver` | `BootReceiver` |
| `CallHandlingService` | `CallHandlingService` |
| `CallAudioController` | `CallAudioController` |
| `RootShell` | `RootShell` |
| `EmailNotifier` | `EmailNotifier` |

## Levels and semantics

| Level | Used for | Examples |
|-------|----------|----------|
| `Log.d` | Normal sequence progress | `onScreenCall` handle, answer scheduled, playback started, call ended, mute applied. |
| `Log.i` | Readiness and root state | Root available or unavailable; responder ready after boot. |
| `Log.w` | Expected degradation and recoverable problems | Missing role, missing permission, battery optimisation enabled, no mixer binary, root command failure, incomplete e-mail settings, no picker available. |
| `Log.e` | Unexpected failures | Answer failure, playback failure, SMTP failure, file import failure, test e-mail failure. |

## Data written to logs

| Data | Where | Sensitivity |
|------|-------|-------------|
| Caller number | `CallScreeningService`, `AutoCallInCallService`, `PhoneStateReceiver` | Personal data; written at debug level. |
| Call state integer | `AutoCallInCallService`, `PhoneStateReceiver` | Not sensitive. |
| Scheduled delays and timings | Both handling components | Not sensitive. |
| SMTP failure detail | `EmailNotifier`, `MainActivity` | Server response text; may contain the recipient address. |
| Root command text | `RootShell` | Command strings are fixed; the mute value is a boolean. |
| Exception stack traces | Several components | Could include file paths or host names. |

The SMTP password is never logged.

## Correlation

There is no correlation identifier, trace identifier, or span. A call can be followed manually through the shared caller number and the ordered messages of a single tag. The dialer and legacy paths use different tags, so an investigation must first determine which path handled the call.

## Sinks and retention

- Sink: logcat only, subject to the device configuration.
- Retention: controlled by the device log buffer, not by the application.
- No log file, rotation, archiving, or sampling exists.

## Audit logging

There is no separate audit log. The durable operator-visible signals are:

- the foreground notification shown by `CallHandlingService`;
- the notification e-mail, which reports the timings and the effective settings.

## Performance

- Logging is synchronous and cheap at the used levels.
- No high-frequency log statement exists in a tight loop.
- Playback and network operations are not logged per frame or per packet.

## Changing log verbosity

There is no runtime switch. To change verbosity, change the `Log.*` calls in the source and rebuild, or use device-level logcat filtering.

## Related documents

- [Error handling](./error-handling.md)
- [Troubleshooting](./troubleshooting.md)
- [Security](./security.md)
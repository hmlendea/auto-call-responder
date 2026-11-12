# Testing

## Strategy

Automated coverage is intentionally narrow: only the pure, Android-free number matcher is unit-tested. Everything else requires a device, a SIM card, and the Telecom roles, so it is verified manually.

## Test inventory

| Test class | Location | Framework | Covers |
|------------|----------|-----------|--------|
| `RomanianPhoneNumbersTests` | `app/src/test/java/ro/nucilandia/autocallresponder/RomanianPhoneNumbersTests.kt` | JUnit 4 | `RomanianPhoneNumbers.isRomanian` |

## What the tests assert

| Test | Inputs | Expectation |
|------|--------|-------------|
| Fixed-line national form | `0376509045`, `0212345678`, `0234567890` | `true` |
| Fixed-line international form | `+40376509045`, `0040376509045`, `40376509045` | `true` |
| Mobile national form | `0722512537`, `0312345678` | `true` |
| Mobile international form | `+40722512537`, `0040722512537` | `true` |
| Formatted numbers | `0722 512 537`, `+40 (722) 512-537`, `tel:+40376509045` | `true` |
| Foreign numbers | `+37360000000`, `0012125551234`, `0612345678` | `false` |
| Incomplete and empty | `""`, `07`, `037650904`, `+40` | `false` |

Note that `0312345678` is asserted as valid although `0[237]` permits a leading `3`; the class is not restricted to mobile prefixes.

## Running

```bash
./gradlew test
./gradlew test --tests "ro.nucilandia.autocallresponder.RomanianPhoneNumbersTests"
```

## Coverage gaps

| Area | Automated coverage |
|------|--------------------|
| Answering sequence (both paths) | None |
| Microphone mute and audio route | None |
| Settings persistence and defaults | None |
| Audio-file import and file provider | None |
| SMTP client and message composition | None |
| Boot receiver readiness checks | None |
| Notification idempotency | None |

## Manual verification checklist

Perform on a device with a SIM card.

1. Grant the dialer and call-screening roles; confirm the main screen hides the permission button.
2. Call from an eligible number; confirm the answer occurs after `delaySeconds`.
3. Confirm the microphone is muted before audio and that the recording starts after the internal delay.
4. Confirm the earpiece route when `earpieceOnly` is enabled.
5. Confirm the call ends one second after the recording finishes.
6. Confirm one notification e-mail with the correct timestamps and settings.
7. Call again and hang up during the ringing delay; confirm no answer occurs and, on the legacy path, that the ended call is still reported.
8. Disable the responder; confirm calls are not answered.
9. Disable the Romanian filter; confirm a foreign number is handled.
10. Remove the audio file; confirm the call ends and the notification is still sent.
11. Corrupt the SMTP password; confirm the failure is logged and does not affect the call.
12. Reboot; confirm the roles survive and the first call is handled immediately.

## Continuous integration

The workflow runs `assembleDebug` and `test`. No device or emulator test runs in CI.

## Related documents

- [Build and deployment](./build-and-deployment.md)
- [Change guide](./change-guide.md)
- [Troubleshooting](./troubleshooting.md)
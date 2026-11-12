# Security

This document complements the root [SECURITY.md](../SECURITY.md) with an implementation-level threat model.

## Trust boundaries

| Boundary | Trust granted | Attacker model |
|----------|---------------|----------------|
| Android Telecom roles | Answer, mute, route, and disconnect calls. | Another application that obtains the dialer or screening role. |
| Runtime permissions | Read phone state, answer calls, record audio, post notifications. | Another application holding the same permissions. |
| Root shell | Arbitrary commands as root. | The device root manager; a compromised `su`. |
| SMTP credentials | Authentication against the configured mail account. | Anyone able to read the preferences file on a rooted or backed-up device. |
| File provider | Read access to the imported audio file. | A malicious document provider the user selects. |
| Logcat | Caller numbers, timestamps, and error details. | Any process able to read logs on the device. |

## Sensitive data

| Data | Storage | Protection |
|------|---------|------------|
| SMTP password | `AutoCallResponderPrefs` (plaintext XML) | Application sandbox only; included in cloud backup. |
| Recipient and sender addresses | `AutoCallResponderPrefs` | Application sandbox only. |
| Caller number | Logcat and the notification body; never persisted by the application | None beyond the OS log policy. |
| Call timestamps | Notification body only | None beyond mail transport. |
| Device manufacturer, model, Android version | Notification body only | None beyond mail transport. |
| Imported audio file | `filesDir/call_audio` | Application sandbox; exposed only through a non-exported `FileProvider`. |

## Threats and mitigations

| Threat | Mitigation | Residual risk |
|--------|------------|---------------|
| Another application binds the exported services | Manifest requires `BIND_SCREENING_SERVICE` and `BIND_INCALL_SERVICE`, which only the system holds | None beyond a compromised system. |
| Another application reads the audio file | `FileProvider` is `exported="false"`; URIs are not broadcast | A component the application explicitly grants could leak the URI; none does. |
| Malicious audio file crashes the player | Playback errors are caught and still end the call and send the notification | A codec exploit in the platform player is out of scope. |
| SMTP credentials exfiltrated through backup | None added | The backup includes the preferences file, including the password. |
| SMTP credentials leaked to logs | The password is never logged | Exception messages from the socket layer could include server responses, not the password. |
| Caller number leaked to logs | Deliberate diagnostic trade-off | Any process that can read logcat can read the number. |
| SMTP downgrade or interception | STARTTLS or implicit TLS; platform trust store | No certificate pinning. |
| Malicious root command injection | Commands are fixed strings; no caller input is interpolated except the boolean mute value | A compromised `su` has full control. |
| Notification body leaked by a third-party mail provider | None added | The recipient and provider determine handling. |
| Denial of service by a hung root prompt | Detection runs on a background thread with a 60-second limit; commands have a 5-second limit | A blocked thread may leak until the timeout. |

## Authorisation

There is no user authentication inside the application. Authorisation is entirely delegated:

- the dialer and call-screening roles are granted by the operating system;
- runtime permissions are granted by the user;
- root is granted by the device root manager;
- the battery exemption is granted by the user through system settings.

## Cryptographic use

- TLS for SMTP is provided by `SSLSocketFactory.getDefault()`, which relies on the platform trust store and the system's enabled protocol versions.
- Base64 is used for SMTP `AUTH LOGIN` and the message body; it is an encoding, not encryption.
- No hashing, key derivation, or symmetric encryption is implemented.

## Hardening recommendations for operators

These are not implemented; they describe how to reduce the residual risks above.

- Exclude the shared-preferences file from cloud backup if the SMTP password must not leave the device.
- Use an application-specific SMTP password rather than the primary account password.
- Restrict physical and ADB access to the device, because logcat carries caller numbers.
- Do not grant root unless uplink routing is genuinely required.

## Related documents

- Root [SECURITY.md](../SECURITY.md)
- [Logging](./logging.md)
- [Integrations](./integrations.md)
- [State and persistence](./state-and-persistence.md)
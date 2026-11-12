# Change guide

How to modify common areas safely. Each section states the files to touch, the invariants to preserve, and the verification to run.

## Change the answering delay

| Files | Action |
|-------|--------|
| `SettingsActivity.kt` | Adjust the seek-bar maximum or the clamp range in `saveSettings`. |
| `activity_settings.xml` | Adjust `android:max` on the seek bar. |
| `README.md` | Update the `delaySeconds` settings row and the configuration table. |

Preserve the clamp to a positive range and the default of `3`. Verify by calling from an eligible number and timing the answer.

## Change the internal delay or the playback timing constants

| Files | Action |
|-------|--------|
| `AutoCallInCallService.kt` | `ANSWER_SETTLE_MILLISECONDS`, `AUDIO_END_DELAY_MILLISECONDS`, `MICROPHONE_MUTE_REAPPLY_MILLISECONDS`. |
| `CallHandlingService.kt` | The same constants plus `AUDIO_PLAYBACK_TIMEOUT_SECONDS`. |

The constants are duplicated across the two paths; change both or extract a shared constant. Preserve the ordering: answer, mute, settle, route, play, mute, delay, disconnect, notify.

## Extend the Romanian number matcher

| Files | Action |
|-------|--------|
| `RomanianPhoneNumbers.kt` | Extend `NATIONAL_PATTERN`, the country-code handling, or the separator regex. |
| `RomanianPhoneNumbersTests.kt` | Add cases for the new grammar; do not weaken existing assertions. |
| `README.md`, `docs/behaviour/automatic-call-answering.md` | Update the documented grammar. |

Verify with `./gradlew test`. The matcher must stay free of Android dependencies so it remains unit-testable.

## Add or rename a setting

| Files | Action |
|-------|--------|
| `SettingsManager.kt` | Add the property to `AppSettings` with a default; never remove or rename without a migration. |
| `SettingsActivity.kt` | Load, edit, and save the value. |
| `activity_settings.xml` | Add the view if needed. |
| `README.md`, `docs/configuration.md`, `docs/data-model.md` | Update the settings tables. |

The Gson field names are the persistence contract. A rename breaks existing installations silently (the old value is ignored). If a rename is unavoidable, read the old key and migrate it.

## Change the notification body

| Files | Action |
|-------|--------|
| `EmailNotifier.kt` | `buildSubject`, `buildBody`, `resolveAudioDescription`. |
| `docs/behaviour/call-notification-emails.md`, `docs/data-model.md` | Update the payload description. |

Preserve the post-disconnect dispatch and the exactly-once guard. Do not add the SMTP password or any secret.

## Change the SMTP transport

| Files | Action |
|-------|--------|
| `EmailSender.kt` | `EmailSender.send` and the private `SmtpConnection`. |

Preserve: implicit TLS only on port 465, STARTTLS otherwise, `AUTH LOGIN`, CRLF termination, base64 body with a UTF-8 content type, and the 20-second connect and read timeouts. Verify manually against a real server with **Test Email**.

## Change microphone mute or audio routing

| Files | Action |
|-------|--------|
| `CallAudioController.kt` | The mute, route, mode, and root routing methods. |

Preserve the preference order: `InCallService`, then root, then `AudioManager` fallback. Keep root mixer commands inside this class. Do not unmute outside the playback window.

## Change the root command catalogue

| Files | Action |
|-------|--------|
| `CallAudioController.kt` | `MIXER_CANDIDATES`, `UPLINK_ROUTES`, `UPLINK_RESTORE_ROUTES`. |

Every applied route must have a matching restore entry. Keep timeouts bounded through `RootShell.execute`.

## Add a permission or component

| Files | Action |
|-------|--------|
| `AndroidManifest.xml` | Declare the permission or component with the correct `exported` value and, for bound services, the platform bind permission. |
| `MainActivity.kt` | Request the runtime permission if it is a dangerous one. |
| `README.md`, `docs/integrations.md`, `docs/security.md` | Update the permission and integration tables. |

## Change the file provider or the audio directory

| Files | Action |
|-------|--------|
| `file_paths.xml` and `AudioFilePicker.kt` | Change the path mapping and the authority together. |
| `AndroidManifest.xml` | The provider authority uses `${applicationId}.fileprovider`. |

## Verification after any change

```bash
./gradlew assembleDebug
./gradlew test
./gradlew lint
```

Then perform the manual checklist in [Testing](./testing.md).

## Related documents

- [Invariants](./invariants.md)
- [Testing](./testing.md)
- [Design decisions](./design-decisions.md)
- [Documentation maintenance](./documentation-maintenance.md)
# Configuration

## Overview

All runtime configuration is stored in the private shared preferences of the application and is edited from `SettingsActivity` or toggled from `MainActivity`. There is no configuration file, environment variable, command-line option, or remote source. Roles, permissions, root access, and the battery-optimisation exemption are configured outside the application through the operating system.

## Sources and precedence

| Setting area | Source | Precedence |
|--------------|--------|------------|
| Responder behaviour | `AppSettings` JSON under key `settings` in `AutoCallResponderPrefs` | Single source; no override. |
| Audio file | `AppSettings.audioFilePath` then `audio_file_uri` preference | `audioFilePath` wins when non-blank. |
| Notification destination and transport | `AppSettings` fields `emailAddress`, `smtpHost`, `smtpPort`, `smtpUser`, `smtpPassword` | Single source. |
| Roles | Android `RoleManager` | Granted by the OS only. |
| Permissions | Android runtime permission state | Granted by the user only. |
| Root | Device root manager | Granted outside the application; cached for the process. |
| Battery optimisation | System power settings | Requested through a system intent; never granted by the application. |

## Settings schema

Source of truth: `AppSettings` in `SettingsManager.kt`.

| Section | Key | Type | Default | Required | Description |
|---------|-----|------|---------|----------|-------------|
| General | `enabled` | `boolean` | `true` | Yes | Enables automatic answering of incoming calls. |
| General | `delaySeconds` | `integer` | `3` | Yes | Seconds to wait before answering; clamped to 1–60 on save. |
| General | `internalDelaySeconds` | `integer` | `2` | Yes | Seconds after answering before audio starts; clamped to 0–60 on save. |
| General | `filterRomanianNumbers` | `boolean` | `true` | Yes | Handles only Romanian fixed-line and mobile numbers when enabled. |
| Audio | `audioFilePath` | `string` | `""` | No | File-provider URI of the imported audio file. |
| Audio | `earpieceOnly` | `boolean` | `true` | Yes | Routes playback to the earpiece instead of the loudspeaker. |
| E-mail | `emailAddress` | `string` | `""` | No | Recipient of the call notification. |
| E-mail | `smtpHost` | `string` | `smtp.gmail.com` | No | SMTP server host. |
| E-mail | `smtpPort` | `integer` | `587` | No | SMTP port; `465` selects implicit TLS, any other port uses STARTTLS. |
| E-mail | `smtpUser` | `string` | `""` | No | SMTP user name, also used as the sender address. |
| E-mail | `smtpPassword` | `string` | `""` | No | SMTP password; stored unencrypted in private preferences. |

## Editing behaviour

`SettingsActivity.saveSettings`:

- `delaySeconds` = parsed value or `3`, clamped to 1–60.
- `internalDelaySeconds` = parsed value or `2`, clamped to 0–60.
- `smtpPort` = parsed value or `587`.
- `emailAddress`, `smtpHost`, `smtpUser` are trimmed; `smtpPassword` is not trimmed.
- Subsequent values are written and the screen closes.

`MainActivity` writes only `enabled` when the switch changes.

The delay seek bar has `android:max="60"` and writes into the hidden `etDelay` field, which is the value that is saved.

## Resolution rules at runtime

| Consumer | Reads | Fallback |
|----------|-------|----------|
| `CallScreeningService.onScreenCall` | `enabled`, `filterRomanianNumbers` | Defaults when no settings exist. |
| `AutoCallInCallService.scheduleAnswer` | `enabled`, `filterRomanianNumbers`, `delaySeconds` | Defaults. |
| `AutoCallInCallService.answer` | `internalDelaySeconds`, `earpieceOnly` | Defaults. |
| `AutoCallInCallService.playAudio` | `audioFilePath`, then `audio_file_uri` | End the call if both are absent. |
| `PhoneStateReceiver.onReceive` | `enabled`, `filterRomanianNumbers`, `delaySeconds` | Defaults. |
| `CallHandlingService.handleCall` | `internalDelaySeconds`, `earpieceOnly`, `audioFilePath`/`audio_file_uri` | Defaults; skip playback if no audio. |
| `EmailNotifier.send` | `emailAddress`, `smtpHost`, `smtpPort`, `smtpUser`, `smtpPassword`, plus `enabled`, delays, filter, `earpieceOnly` for the body | Suppress the message when recipient, user, or password is blank. |

## Secret handling

The SMTP password is the only secret. It is stored in `AutoCallResponderPrefs`, which is inside the application sandbox but is not additionally encrypted. It is never written to logcat. The password field on the settings screen uses `inputType="textPassword"`.

## Backup

`backup_rules.xml` and `data_extraction_rules.xml` include only `domain="sharedpref" path="AutoCallResponderPrefs.xml"` for cloud backup. The imported audio file is not included in the backup rules.

## Related documents

- [Settings and storage component](./components/settings-and-storage.md)
- [State and persistence](./state-and-persistence.md)
- [Data model](./data-model.md)
- [README configuration section](../README.md#️-configuration)
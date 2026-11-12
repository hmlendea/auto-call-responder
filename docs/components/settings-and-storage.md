# Settings and storage component

## `SettingsManager`

`object SettingsManager` in `SettingsManager.kt`.

### Constants

| Constant | Value |
|----------|-------|
| `PREFS_NAME` | `AutoCallResponderPrefs` |
| `KEY_SETTINGS` | `settings` |
| `KEY_AUDIO_FILE_URI` | `audio_file_uri` |

### State

A `lateinit var sharedPreferences` bound by `init(context)` and a private `Gson` instance.

### API

| Method | Behaviour |
|--------|-----------|
| `init(context)` | Binds `getSharedPreferences(PREFS_NAME, MODE_PRIVATE)`. Called by the application and by `BootReceiver`. |
| `getSettings()` | Returns the parsed `AppSettings`, or a default instance when the key is absent. |
| `saveSettings(settings)` | Serialises and applies. |
| `getAudioFileUri()` | Returns the stored URI or `null`. |
| `saveAudioFileUri(uri)` | Stores the URI. |
| `clearAudioFileUri()` | Removes the key. |

### Constraints

- `init` must run before any other method; `AutoCallResponderApplication.onCreate` guarantees this for a normal process start, and `BootReceiver` re-initialises defensively.
- There is no schema version and no migration path. Gson field names are the persistence contract.
- A malformed `settings` JSON raises during parsing and is not caught.

## `AppSettings`

The persisted record; see [Data model](../data-model.md#appsettings) for the field table and defaults.

## Audio-file store

| Property | Value |
|----------|-------|
| Directory | `filesDir/call_audio` |
| File name | `selected_call_audio.<ext>` |
| Extension | MIME subtype of the source, filtered to alphanumerics, default `mp3` |
| Retention | Exactly one file; the directory is purged on each import |
| Exposure | `content://${applicationId}.fileprovider/call_audio/<file>` |

The clear action removes the stored URIs but does not delete the file; the next import purges it.

## Preferences backup

| File | Included content |
|------|------------------|
| `backup_rules.xml` | `AutoCallResponderPrefs.xml` |
| `data_extraction_rules.xml` | `AutoCallResponderPrefs.xml` under `<cloud-backup>` |

The audio file is not backed up. A restore therefore produces a selected-audio state without the underlying file.

## Consumers

| Consumer | Reads | Writes |
|----------|-------|--------|
| `MainActivity` | `enabled`, `delaySeconds`, `emailAddress`, audio state | `enabled`, `audioFilePath`, `audio_file_uri` |
| `SettingsActivity` | all fields | all fields, audio URIs |
| `CallScreeningService` | `enabled`, `filterRomanianNumbers` | — |
| `AutoCallInCallService` | `enabled`, `filterRomanianNumbers`, delays, `earpieceOnly`, audio | — |
| `PhoneStateReceiver` | `enabled`, `filterRomanianNumbers`, `delaySeconds` | — |
| `CallHandlingService` | `internalDelaySeconds`, `earpieceOnly`, audio | — |
| `EmailNotifier` | e-mail fields and the effective settings for the body | — |
| `BootReceiver` | `enabled` | — |

## Related documents

- [Configuration](../configuration.md)
- [State and persistence](../state-and-persistence.md)
- [Data model](../data-model.md)
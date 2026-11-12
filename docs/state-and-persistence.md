# State and persistence

## Stores

| Store | Owner | Representation | Scope |
|-------|-------|----------------|-------|
| `AutoCallResponderPrefs` | `SettingsManager` | XML shared preferences file with keys `settings` (JSON) and `audio_file_uri` (string) | Persistent, private to the application. |
| `filesDir/call_audio` | `AudioFilePicker` | One file `selected_call_audio.<ext>` | Persistent, private to the application. |
| Root availability | `RootShell` | `@Volatile Boolean?` | Process lifetime only. |
| Call sessions | `AutoCallInCallService`, `CallHandlingService` | Kotlin objects | One call only. |
| Media player | Both handling components | `MediaPlayer?` | One call only. |

## Initialisation

`AutoCallResponderApplication.onCreate` calls `SettingsManager.init(this)`, which resolves `getSharedPreferences("AutoCallResponderPrefs", MODE_PRIVATE)` and stores it in a `lateinit` field. Because the field is `lateinit`, every other component depends on the application having been created first; a process that starts the application object before any component always satisfies this.

`BootReceiver.onReceive` calls `SettingsManager.init(context)` again, which is idempotent and re-binds the preferences after a reboot or package replacement.

## Read and write paths

| Operation | Method | Effect |
|-----------|--------|--------|
| Read the settings record | `SettingsManager.getSettings()` | Parses the `settings` JSON with Gson, or returns a default `AppSettings()`. |
| Write the settings record | `SettingsManager.saveSettings(settings)` | Serialises to JSON and applies asynchronously. |
| Read the audio URI | `SettingsManager.getAudioFileUri()` | Returns the stored string or `null`. |
| Write the audio URI | `SettingsManager.saveAudioFileUri(uri)` | Stores the string. |
| Clear the audio URI | `SettingsManager.clearAudioFileUri()` | Removes the key. |
| Import audio | `AudioFilePicker.import` | Purges the directory, copies the file, returns a file-provider URI. |

Writes use `apply()` (asynchronous commit), so a settings change is visible to subsequent reads in the same process immediately but may be flushed to disk slightly later.

## Lifecycle of the imported audio

1. The user selects an audio file; `AudioFilePicker.launch` starts the first picker that accepts `audio/*`.
2. `import` deletes every existing file in `filesDir/call_audio`, resolves the extension from the MIME type, and copies the source stream into the target file.
3. The file-provider URI is written to `AppSettings.audioFilePath` and to `audio_file_uri`.
4. Playback resolves `audioFilePath` first, then `audio_file_uri`.
5. `SettingsActivity.btnClearAudio` clears both values, but does not delete the file from disk; the next import purges it.

## Backup and restore

| File | Inclusion |
|------|-----------|
| `backup_rules.xml` | `domain="sharedpref" path="AutoCallResponderPrefs.xml"` |
| `data_extraction_rules.xml` | `<cloud-backup>` includes the same shared-preferences file. |

Consequences:

- The settings, including the SMTP password, are included in cloud backup. Restoring a backup restores the configuration and the credentials.
- The imported audio file is not included, so a restored installation shows the audio URI as selected while the file is absent. Playback then fails and is handled as a playback error.

## Cache invalidation

- `RootShell.availability` is cleared by `requestAccess` and re-detected; it is not invalidated when the user revokes root.
- The settings are re-read at every decision point, so an external change (for example through a backup restore while the process lives) is picked up on the next read only after the process is restarted, because `SettingsManager` holds the same `SharedPreferences` instance that observes external file changes only through the platform listener, which is not registered.

## Related documents

- [Configuration](./configuration.md)
- [Data model](./data-model.md)
- [Settings and storage component](./components/settings-and-storage.md)
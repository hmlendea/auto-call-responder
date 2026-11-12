# Presentation component

## Components

| Class | File | Role |
|-------|------|------|
| `MainActivity` | `MainActivity.kt` + `activity_main.xml` | Launcher screen; enable switch, status, permission and role requests, audio selection, test e-mail, donation link. |
| `SettingsActivity` | `SettingsActivity.kt` + `activity_settings.xml` | Edit delays, e-mail and SMTP settings, filters, audio route, audio file, root, and battery exemption. |
| `AudioFilePicker` | `AudioFilePicker.kt` | Launch a document picker and import the selected document. |
| `AutoCallResponderApplication` | `AutoCallResponderApplication.kt` | Composition root; caches the instance and initialises settings. |

## `MainActivity`

### Views

| Identifier | Type | Purpose |
|------------|------|---------|
| `tvStatus` | `TextView` | "Status: Enabled/Disabled", coloured green or red. |
| `swEnabled` | `Switch` | Master switch; writes `AppSettings.enabled`. |
| `tvDelay` | `TextView` | Effective answering delay. |
| `tvEmail` | `TextView` | Recipient address or "Not set". |
| `tvAudioFile` | `TextView` | "Selected" or "Not selected". |
| `btnSettings` | `Button` | Opens `SettingsActivity`. |
| `btnSelectAudio` | `Button` | Launches `AudioFilePicker.launch`. |
| `btnTestEmail` | `Button` | Sends a test message through `EmailSender`. |
| `btnRequestPermissions` | `Button` | Visible only when a permission or role is missing. |
| `btnDonate` | `Button` | Opens `https://hmlendea.go.ro/funding`. |

### Lifecycle

- `onCreate`: `initViews`, `setupListeners`, `updateUI`, `checkPermissions`.
- `onResume`: `updateUI`.
- `onActivityResult`: handles `AudioFilePicker.REQUEST_CODE` (1001) and stores the imported file.
- `onRequestPermissionsResult`: hides the permission button when every requested permission is granted.

### Permission and role logic

`checkPermissions` reveals the request button when any of these is missing:

- dialer role (Android 10+);
- `READ_PHONE_STATE`;
- `ANSWER_PHONE_CALLS`;
- `POST_NOTIFICATIONS` (Android 13+);
- `RECORD_AUDIO`.

`requestPermissions` requests the missing runtime permissions first, then:

- requests the dialer role if it is not held;
- otherwise requests the call-screening role if it is not held.

### Test e-mail

`sendTestEmail` validates the recipient and the SMTP user and password, disables the button, and runs `EmailSender.send` on `Dispatchers.IO` through `lifecycleScope.launch`. Success and failure are reported with toasts; the exception message is included in the failure toast.

## `SettingsActivity`

### Views

| Identifier | Type | Purpose |
|------------|------|---------|
| `etDelay` | `EditText` (hidden) | Numeric value synchronised with the seek bar and saved. |
| `seekBarDelay` + `tvDelayValue` | `SeekBar` + `TextView` | Delay selection 0–60; the saved value is clamped to 1–60. |
| `etInternalDelay` | `EditText` | Delay after answering. |
| `swEarpiece` | `SwitchCompat` | Earpiece-only playback. |
| `etEmail` | `EditText` | Notification recipient. |
| `etSmtpHost`, `etSmtpPort`, `etSmtpUser`, `etSmtpPassword` | `EditText` | SMTP transport configuration. |
| `tvAudioFile` | `TextView` | Audio selection state. |
| `btnSelectAudio`, `btnClearAudio` | `Button` | Import or clear the audio file. |
| `swFilterRomanian` | `SwitchCompat` | Romanian-number filter. |
| `tvRootStatus`, `btnGrantRoot` | `TextView` + `Button` | Root detection and request. |
| `btnBatteryExemption` | `Button` | Opens the battery-optimisation settings. |
| `btnSave`, `btnCancel` | `Button` | Persist and close, or close without saving. |

### Save behaviour

`saveSettings` re-reads the current settings, applies the edited values with the clamps described in [Configuration](../configuration.md), persists them, shows a toast, and closes the screen. The audio file is saved immediately on import or clear, independently of Save.

### Battery exemption

`requestBatteryExemption` opens `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` with `package:<packageName>`, falling back to `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`, then to a toast.

## `AudioFilePicker`

| Member | Behaviour |
|--------|-----------|
| `REQUEST_CODE` = `1001` | Result code expected by both activities. |
| `launch(activity)` | Tries `ACTION_GET_CONTENT`, then `ACTION_OPEN_DOCUMENT`, then `ACTION_PICK` with `audio/*`; catches `ActivityNotFoundException` and general exceptions; shows a toast when none works. |
| `import(context, sourceUri)` | Purges `filesDir/call_audio`, derives the extension from the MIME subtype, copies the stream, and returns `FileProvider.getUriForFile(context, "${packageName}.fileprovider", file)`. Returns `null` and shows a toast on failure. |

## UI text

Most user-visible strings are hardcoded in the Kotlin sources and layouts. `strings.xml` defines the application name, the settings title, and a set of labels that are not all referenced by the layouts.

## Related documents

- [Settings and storage](./settings-and-storage.md)
- [Behaviour: automatic call answering](../behaviour/automatic-call-answering.md)
- [Flow: startup and readiness](../flows/startup-and-readiness.md)
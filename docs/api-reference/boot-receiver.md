# BootReceiver

Receiver for `android.intent.action.BOOT_COMPLETED`, `android.intent.action.QUICKBOOT_POWERON`, and `android.intent.action.MY_PACKAGE_REPLACED`, declared exported with priority 1000.

Implementation: `ro.nucilandia.autocallresponder.BootReceiver`, file `BootReceiver.kt`.

## onReceive(context: Context, intent: Intent)

### Request

| Input | Type | Values |
|-------|------|--------|
| `intent.action` | `String?` | One of the three boot-related actions; anything else causes an immediate return. |

### Behaviour

| Step | Effect |
|------|--------|
| 1 | `SettingsManager.init(context)`. |
| 2 | On Android 10+, read `RoleManager` and log when the dialer or call-screening role is missing. |
| 3 | Read `PowerManager.isIgnoringBatteryOptimizations(packageName)` and log when optimisation is enabled. |
| 4 | When the responder is enabled, log readiness and call `RootShell.detectAsync()`. |
| 5 | When the responder is disabled, log that it is disabled. |

### Side effects

- Log entries.
- A background root-detection attempt, which may show a root-manager prompt.
- No service is started, and no persistent state is written.

### Guarantees

| Guarantee | Reason |
|-----------|--------|
| No long-lived work | Only verification and an asynchronous detection are performed. |
| Settings are available to the first call after a reboot | `SettingsManager.init` re-binds the preferences. |
| Telecom components remain available | Transport is delegated to Telecom, which binds the services because the roles survived. |

### Error scenarios

| Scenario | Behaviour |
|----------|-----------|
| Role check unavailable | Logged as a warning; no corrective action. |
| Root unavailable | Logged by `RootShell`; the responder still works with local playback. |

## Flow references

- [Flow: startup and readiness](../flows/startup-and-readiness.md)
- [Root integration component](../components/root-integration.md)
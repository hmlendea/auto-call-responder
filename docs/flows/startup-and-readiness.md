# Flow: startup and readiness

## Process start

```mermaid
sequenceDiagram
    participant System as Android system
    participant App as AutoCallResponderApplication
    participant Settings as SettingsManager
    participant Lifecycle as ProcessLifecycleOwner

    System->>App: onCreate
    App->>App: appInstance = this
    App->>Settings: init(this)
    Settings->>Settings: bind AutoCallResponderPrefs
    App->>Lifecycle: addObserver(AppLifecycleObserver)
```

`AppLifecycleObserver` has empty `ON_START` and `ON_STOP` callbacks, so it has no behavioural effect beyond registration.

## Activity start

`MainActivity.onCreate` initialises views, wires listeners, paints the status from the current settings, and evaluates permissions to decide whether the request button is visible. `onResume` repaints the status, so returning from the settings screen reflects saved changes.

`SettingsActivity.onCreate` initialises views, loads the current settings into the controls, and wires listeners. When root state is already cached, the root status text is updated immediately.

## Role and permission readiness

```mermaid
flowchart TD
    Start["MainActivity.checkPermissions"] --> DialerRole{"Dialer role held? (Android 10+)"}
    DialerRole -- no --> Show["Show Request Permissions button"]
    DialerRole -- yes --> Perms{"Runtime permissions missing?"}
    Perms -- yes --> Show
    Perms -- no --> Hidden["Keep the button hidden"]
    Show --> Request["User taps Request Permissions"]
    Request --> RuntimePerms["Request READ_PHONE_STATE, ANSWER_PHONE_CALLS, POST_NOTIFICATIONS (13+), RECORD_AUDIO"]
    RuntimePerms --> RoleChoice{"Dialer role held?"}
    RoleChoice -- no --> DialerReq["Request ROLE_DIALER, or open default-apps settings"]
    RoleChoice -- yes --> ScreenHeld{"Call-screening role held?"}
    ScreenHeld -- no --> ScreenReq["Request ROLE_CALL_SCREENING"]
    ScreenHeld -- yes --> Done["Nothing further to request"]
```

## Boot and update restoration

```mermaid
sequenceDiagram
    participant System as Android system
    participant Boot as BootReceiver
    participant Settings as SettingsManager
    participant Roles as RoleManager
    participant Power as PowerManager
    participant Root as RootShell

    System->>Boot: BOOT_COMPLETED / QUICKBOOT_POWERON / MY_PACKAGE_REPLACED
    Boot->>Settings: init(context)
    Boot->>Roles: isRoleHeld(ROLE_DIALER), isRoleHeld(ROLE_CALL_SCREENING)
    Boot->>Power: isIgnoringBatteryOptimizations(packageName)
    alt responder enabled
        Boot->>Root: detectAsync()
    else responder disabled
        Boot->>Boot: log that it is disabled
    end
```

The receiver starts no service. Telecom binds the screening and in-call services on its own when the roles survive the reboot, so the only useful work is verification and the early root detection that avoids a prompt during the first call.

## Readiness prerequisites

| Prerequisite | Needed for | Checked by |
|--------------|------------|------------|
| `ROLE_DIALER` | AutoCallInCallService binding, supported mute and route | `MainActivity`, `BootReceiver`, `PhoneStateReceiver` |
| `ROLE_CALL_SCREENING` | CallScreeningService binding | `MainActivity`, `BootReceiver` |
| `READ_PHONE_STATE` | Legacy call observation | `MainActivity` |
| `ANSWER_PHONE_CALLS` | Answering and ending calls | `MainActivity`, `CallHandlingService` |
| `RECORD_AUDIO` | Audio path during the call | `MainActivity` |
| `POST_NOTIFICATIONS` (Android 13+) | Foreground notification visibility | `MainActivity` |
| Battery-optimisation exemption | On-time scheduled answer | `BootReceiver` (reported) |

## Related documents

- [Presentation component](../components/presentation.md)
- [Call observation component](../components/call-observation.md)
- [Troubleshooting](../troubleshooting.md)
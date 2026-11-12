# Root integration component

## Object

`internal object RootShell` in `RootShell.kt`.

## Constants

| Constant | Value | Purpose |
|----------|-------|---------|
| `DETECT_TIMEOUT_MILLISECONDS` | `60_000` | Limit for the detection command. |
| `COMMAND_TIMEOUT_MILLISECONDS` | `5_000` | Default limit for ordinary commands. |

## State

| Field | Declaration | Meaning |
|-------|-------------|---------|
| `availability` | `@Volatile Boolean?` | `null` until detected, then `true` or `false`. |
| `detectionInProgress` | `AtomicBoolean` | Prevents concurrent detections. |
| `mainHandler` | `Handler(Looper.getMainLooper())` | Delivers callbacks on the main thread. |

## API

| Method | Behaviour |
|--------|-----------|
| `cachedAvailability` | Returns the cached value or `null`. |
| `isAvailable()` | Returns `availability == true`; never triggers detection. |
| `detectAsync(onResult)` | Ignores the call when a detection is already in progress; otherwise starts a `RootShell-Detect` thread, runs detection, clears the flag, and posts the result to the main thread. |
| `requestAccess(onResult)` | Clears the cached value and calls `detectAsync`. |
| `execute(command, timeoutMillis)` | Returns `null` when root is not cached as available; otherwise runs `su -c <command>` with `redirectErrorStream`, waits for the timeout, destroys the process on expiry, and returns the combined output. |

## Detection

`runDetection` runs `id` through `su` and treats the output as granted when it contains `uid=0`. The result is cached and logged.

## Callers

| Caller | Purpose |
|--------|---------|
| `BootReceiver` | Warms detection after a reboot when the responder is enabled. |
| `SettingsActivity` | `requestRootAccess` prompts the root manager and updates the status text. |
| `CallAudioController` | Microphone-mute fallback and uplink routing. |

## Timeouts and failure

- A detection that exceeds 60 seconds is destroyed and cached as unavailable.
- A command that exceeds its timeout is destroyed and returns `null`.
- Every process start and wait is wrapped; exceptions are logged and return `null`.
- Detection never blocks the caller; callers rely on the cached value.

## Security notes

- Commands are fixed strings; no user input is interpolated except the boolean mute value.
- Root access grants arbitrary command execution as root; the application restricts itself to the mute command and the mixer catalogue.
- The cached state is not invalidated when the user revokes root; `requestAccess` performs the explicit reset.

## Related documents

- [Audio control](./audio-control.md)
- [Security](../security.md)
- [Logging](../logging.md)
# Dependencies

## Build toolchain

| Tool | Version | Source |
|------|---------|--------|
| Gradle | 8.10.2 | `gradle/wrapper/gradle-wrapper.properties` |
| Android Gradle Plugin | 8.7.0 | `build.gradle.kts` |
| Kotlin Android plugin | 2.0.21 | `build.gradle.kts` |
| Java source/target | 17 | `app/build.gradle.kts` |
| Kotlin JVM target | 17 | `app/build.gradle.kts` |
| compileSdk / targetSdk | 36 | `app/build.gradle.kts` |
| minSdk | 26 | `app/build.gradle.kts` |

## Runtime dependencies

Declared in `app/build.gradle.kts`. Scope is `implementation` unless stated otherwise.

| Package | Version | Scope | Purpose |
|---------|---------|-------|---------|
| `androidx.core:core-ktx` | 1.15.0 | Runtime | Kotlin extensions, `ContextCompat`, `FileProvider`, notification compatibility. |
| `androidx.appcompat:appcompat` | 1.7.0 | Runtime | Activity base classes and AppCompat widgets (`SwitchCompat`). |
| `com.google.android.material:material` | 1.12.0 | Runtime | Material theme (`Theme.MaterialComponents.DayNight.DarkActionBar`). |
| `androidx.constraintlayout:constraintlayout` | 2.1.4 | Runtime | The layout of both activities. |
| `androidx.lifecycle:lifecycle-runtime-ktx` | 2.8.7 | Runtime | Lifecycle-aware coroutine scopes. |
| `androidx.lifecycle:lifecycle-viewmodel-ktx` | 2.8.7 | Runtime | Declared; no `ViewModel` is used. |
| `androidx.lifecycle:lifecycle-process` | 2.8.7 | Runtime | `ProcessLifecycleOwner` used by the application class. |
| `androidx.preference:preference-ktx` | 1.2.1 | Runtime | Declared; the settings screen uses plain views and raw preferences. |
| `com.google.code.gson:gson` | 2.11.0 | Runtime | Serialise and deserialise `AppSettings`. |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.9.0 | Runtime | IO dispatcher, delays, cancellation, `CompletableDeferred`. |
| `junit:junit` | 4.13.2 | Test | The unit-test framework used by `RomanianPhoneNumbersTests`. |

## Platform APIs relied upon

| API | Minimum API level | Used by |
|-----|-------------------|---------|
| `android.telecom.CallScreeningService` | 29 | `CallScreeningService` |
| `android.telecom.InCallService` | 23 | `AutoCallInCallService` |
| `android.telecom.Call.answer` / `disconnect` | 23 | `AutoCallInCallService` |
| `android.telecom.TelecomManager.acceptRingingCall` | 26 | `CallHandlingService` |
| `android.telecom.TelecomManager.endCall` | 28 | `CallHandlingService` |
| `android.app.role.RoleManager` | 29 | `MainActivity`, `BootReceiver`, `PhoneStateReceiver` |
| `android.media.MediaPlayer` | 1 | Both handling components |
| `androidx.core.content.FileProvider` | (AndroidX) | `AudioFilePicker` |
| `java.util.Base64` | 26 | `EmailSender` |
| `javax.net.ssl.SSLSocketFactory` | 1 | `EmailSender` |

## Transitive or implicit dependencies

| Dependency | Reason |
|------------|--------|
| Kotlin standard library | Pulled in by the Kotlin Android plugin. |
| `androidx.activity` (transitive) | Provided by `appcompat`; `startActivityForResult` is inherited from `ComponentActivity`. |
| Android platform `SharedPreferences`, `NotificationManager`, `AudioManager`, `Handler` | No external dependency; part of the platform. |

## Unused or redundant declarations

- `androidx.lifecycle:lifecycle-viewmodel-ktx` and `androidx.preference:preference-ktx` are declared but not exercised by the current source.
- `android.enableJetifier=true` is set although only AndroidX dependencies are used; it is harmless but unnecessary.
- `AppLifecycleObserver` is registered with `ProcessLifecycleOwner` but both callbacks are empty.

## Dependency rule

Dependencies point inward from Android components towards helpers. Helpers never depend on an activity, service, or receiver. The only outward crossings are platform and network calls inside the helpers listed in [Architecture](./architecture.md#ownership-boundaries).

## Related documents

- [Build and deployment](./build-and-deployment.md)
- [Integrations](./integrations.md)
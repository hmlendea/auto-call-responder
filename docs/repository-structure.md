# Repository structure

## Top level

| Path | Purpose |
|------|---------|
| `build.gradle.kts` | Root Gradle build; declares the Android application and Kotlin plugins without applying them. |
| `settings.gradle.kts` | Plugin and dependency repositories; sets the root project name and includes `:app`. |
| `gradle.properties` | JVM arguments and AndroidX/Jetifier flags. |
| `gradle/wrapper/gradle-wrapper.properties` | Pins Gradle 8.10.2. |
| `gradlew`, `gradlew.bat` | Gradle wrapper launchers. |
| `app/` | The single application module. |
| `README.md`, `ARCHITECTURE.md`, `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, `SECURITY.md`, `PRIVACY.md`, `LICENSE` | Root documents. |
| `.github/workflows/android.yml` | Build-and-test workflow. |
| `.github/workflows/github-release.yml` | Release workflow that builds the unsigned release APK and attaches it to a published GitHub release. |
| `.github/FUNDING.yml` | Funding metadata used by the README badge. |
| `docs/` | This knowledge base. |

## Application module

```
app/
├── build.gradle.kts              # Android application configuration and dependencies
├── proguard-rules.pro            # ProGuard rules (minification disabled in release)
└── src/
    ├── main/
    │   ├── AndroidManifest.xml   # Components, permissions, and the file provider
    │   ├── java/ro/nucilandia/autocallresponder/
    │   │   ├── AutoCallResponderApplication.kt
    │   │   ├── MainActivity.kt
    │   │   ├── SettingsActivity.kt
    │   │   ├── AudioFilePicker.kt
    │   │   ├── SettingsManager.kt
    │   │   ├── RomanianPhoneNumbers.kt
    │   │   ├── CallScreeningService.kt
    │   │   ├── AutoCallInCallService.kt
    │   │   ├── PhoneStateReceiver.kt
    │   │   ├── BootReceiver.kt
    │   │   ├── CallHandlingService.kt
    │   │   ├── CallAudioController.kt
    │   │   ├── RootShell.kt
    │   │   ├── EmailNotifier.kt
    │   │   └── EmailSender.kt
    │   └── res/
    │       ├── drawable/ic_launcher_foreground.xml
    │       ├── layout/activity_main.xml
    │       ├── layout/activity_settings.xml
    │       ├── mipmap-anydpi-v26/ic_launcher.xml
    │       ├── mipmap-anydpi-v26/ic_launcher_round.xml
    │       ├── values/colors.xml
    │       ├── values/strings.xml
    │       ├── values/themes.xml
    │       └── xml/
    │           ├── backup_rules.xml
    │           ├── data_extraction_rules.xml
    │           └── file_paths.xml
    └── test/
        └── java/ro/nucilandia/autocallresponder/
            └── RomanianPhoneNumbersTests.kt
```

## Layer-to-file mapping

| Layer | Files | Notes |
|-------|-------|-------|
| Composition root | `AutoCallResponderApplication.kt` | Caches the instance and initialises `SettingsManager`. |
| Presentation | `MainActivity.kt`, `SettingsActivity.kt`, `AudioFilePicker.kt` | User interaction, permission and role requests, audio import. |
| Call observation | `CallScreeningService.kt`, `AutoCallInCallService.kt`, `PhoneStateReceiver.kt`, `BootReceiver.kt` | Detect and classify calls; restore readiness. |
| Call orchestration | `CallHandlingService.kt`, `CallAudioController.kt` | Drive the sequence and control audio for the legacy path and audio, respectively. |
| Platform and support | `SettingsManager.kt`, `RomanianPhoneNumbers.kt`, `RootShell.kt`, `EmailNotifier.kt`, `EmailSender.kt` | Persistence, matching, privileged execution, notification. |

## Naming conventions in the source

- Activities end in `Activity`; Android services end in `Service`; broadcast receivers end in `Receiver`.
- Helper objects that hold no state are declared as Kotlin `object` singletons (`SettingsManager`, `AudioFilePicker`, `RomanianPhoneNumbers`, `RootShell`, `EmailNotifier`).
- The audio helper is an `internal class` because it is created per call.
- The SMTP transport is a `class` constructed per message; its session helper `SmtpConnection` is `private`.

## Related documents

- [Repository overview](./repository-overview.md)
- [Architecture](./architecture.md)
- [Components](./components/presentation.md)

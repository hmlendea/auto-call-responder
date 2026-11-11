[![License](https://img.shields.io/github/license/hmlendea/auto-call-responder)](https://github.com/hmlendea/auto-call-responder/blob/master/LICENSE)

# Auto Call Responder

An Android application that answers incoming calls automatically after a configurable delay, plays an audio file during the call, and sends a notification e-mail describing the call.

## 📑 Table of Contents

- [Capabilities](#-capabilities)
- [Usage](#-usage)
- [Known Limitations](#️-known-limitations)
- [System Requirements](#️-system-requirements)
- [Installation](#-installation)
  - [Installation from Source](#installation-from-source)
- [Configuration](#️-configuration)
  - [Settings](#settings)
- [Compatibility](#-compatibility)
- [Integrations](#-integrations)
- [Permissions](#-permissions)
- [Privacy and Data](#️-privacy-and-data)
- [Development](#️-development)
  - [Requirements](#requirements)
  - [Setup](#setup)
  - [Build](#build)
  - [Run](#run)
  - [Linting](#linting)
  - [Test](#test)
- [Troubleshooting](#-troubleshooting)
- [Contributing](#-contributing)
- [Support](#-support)
- [Project Engagement](#-project-engagement)
- [License](#-license)

## ✨ Capabilities

- Answers incoming calls automatically after a configurable delay.
- Plays a selected audio file from the device during the handled call.
- Sends an e-mail notification with the caller number, timestamps, and the effective settings.
- Handles calls from any SIM card on the device, without a per-SIM configuration.
- Filters calls by Romanian numbers so that only fixed-line and mobile numbers in national, `+40`, or `0040` form are handled.
- Stops the audio and skips every remaining delay as soon as the call ends, while still sending the notification.
- Mutes the microphone for the entire call, so ambient sound is never sent to the caller.
- Supports playing the audio through the earpiece only, keeping the loudspeaker disabled.
- Restores the settings and roles after a device reboot.

## 🚀 Usage

Install the application, grant the requested permissions, and assign the dialer and call-screening roles when the application asks for them.

1. Open the application.
2. Tap **Settings** and configure the answering delay, the audio file, the e-mail address, and the SMTP account.
3. Tap **Save**.
4. Enable the responder with the main switch.
5. Tap **Request Permissions** if the application reports missing permissions or roles.

Every incoming call that matches the active filter is then answered after the configured delay, and the notification e-mail is sent once the call has been disconnected.

## ⚠️ Known Limitations

- On Android 10 and later, call screening only works while the application holds the dialer and call-screening roles.
- Public Android APIs do not allow an application to inject audio into the voice call uplink, so the other party cannot hear the recording; root access and a mixer control exposed by the device kernel are required for that route.
- Automatic e-mail delivery requires an SMTP account with the credentials entered in the settings.
- The SMTP password is stored in the private shared preferences of the application, which are not encrypted.
- Ending the call depends on the `ANSWER_PHONE_CALLS` permission and, on Android 10 and later, on the dialer role.
- The application cannot read the incoming number on devices that restrict the caller identifier.

## 🖥️ System Requirements

| Component | Minimum | Recommended |
|-----------|---------|-------------|
| Android | 8.0 (API 26) | 10 (API 29) or later |
| Telephony hardware | A device that can receive calls | A device with a SIM card |

## 📦 Installation

### Installation from Source

```bash
git clone https://github.com/hmlendea/auto-call-responder.git
cd auto-call-responder
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## ⚙️ Configuration

The application is configured from its own settings screen. The values are stored in the private shared preferences of the application and are applied to the next handled call.

### Settings

The subsequent settings are recognised:
| Section | Key | Type | Default | Required | Description |
|---------|-----|------|---------|----------|-------------|
| General | `enabled` | `boolean` | `true` | Yes | Enables the automatic answering of incoming calls. |
| General | `delaySeconds` | `integer` | `3` | Yes | Seconds to wait before answering, between 1 and 60. |
| General | `internalDelaySeconds` | `integer` | `2` | Yes | Seconds to wait after answering before the audio starts, between 0 and 60. |
| General | `filterRomanianNumbers` | `boolean` | `true` | Yes | Handles only Romanian fixed-line and mobile numbers in national, `+40`, or `0040` form. |
| Audio | `audioFilePath` | `string` | — | No | Audio file played during the handled call. |
| Audio | `earpieceOnly` | `boolean` | `true` | Yes | Plays the audio through the earpiece instead of the loudspeaker. |
| E-mail | `emailAddress` | `string` | — | No | Recipient of the call notification. |
| E-mail | `smtpHost` | `string` | `smtp.gmail.com` | No | Host of the SMTP server used to send the notification. |
| E-mail | `smtpPort` | `integer` | `587` | No | Port of the SMTP server; port 465 selects implicit TLS. |
| E-mail | `smtpUser` | `string` | — | No | User name, also used as the sender address. |
| E-mail | `smtpPassword` | `string` | — | No | Password of the SMTP account. |

## 🧩 Compatibility

| Component | Supported Versions | Notes |
|-----------|--------------------|-------|
| Android | 8.0 (API 26) and later | Calls are observed through a `PHONE_STATE` broadcast receiver on Android 8 and 9. |
| Android | 10 (API 29) and later | Calls are observed through `CallScreeningService`, which requires the dialer and call-screening roles. |
| SMTP servers | Servers that accept STARTTLS on the configured port, or implicit TLS on port 465 | Authentication is performed with `AUTH LOGIN`. |

## 🔌 Integrations

| Integration | Compatibility | Purpose | Required |
|-------------|---------------|---------|----------|
| SMTP server | STARTTLS or implicit TLS on port 465 | Sends the call notification e-mail after the call has been disconnected. | No |

## 🔑 Permissions

| Permission | Required | Purpose |
|------------|----------|---------|
| `ANSWER_PHONE_CALLS` | Yes | Answers the incoming call. |
| `READ_PHONE_STATE` | Yes | Detects the state changes of the incoming call. |
| `MANAGE_OWN_CALLS` | Yes | Allows the application to manage the calls it handles. |
| `CALL_PHONE` | Yes | Allows the call handling sequence performed through the telephony framework. |
| `FOREGROUND_SERVICE` | Yes | Keeps call handling alive while the call is in progress. |
| `FOREGROUND_SERVICE_PHONE_CALL` | Yes | Declares the phone call type of the foreground service. |
| `RECORD_AUDIO` | Yes | Opens the audio path used to play the recording during the call. |
| `MODIFY_AUDIO_SETTINGS` | Yes | Adjusts the call audio route and the stream volumes. |
| `INTERNET` | Yes | Reaches the SMTP server for notifications. |
| `RECEIVE_BOOT_COMPLETED` | Yes | Restores the responder state after a reboot. |
| `POST_NOTIFICATIONS` | No | Shows the foreground service notification on Android 13 and later. |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | No | Allows the user to exempt the application from battery optimisation. |

## 🛡️ Privacy and Data

Auto Call Responder processes call metadata and the e-mail account details entered by the user, and it stores the settings on the device.

| Data | Purpose | Storage | Retention | Optional |
|------|---------|---------|-----------|----------|
| Caller number and call timestamps | Composing the call notification e-mail. | Memory only, during the handled call. | Until the call ends. | Yes |
| Recipient e-mail address | Sending the call notification. | Private shared preferences of the application. | Until the settings are changed. | Yes |
| SMTP host, port, user name, and password | Authenticating against the SMTP server. | Private shared preferences of the application. | Until the settings are changed. | Yes |
| Answering delay, filters, and audio selection | Driving the answering behaviour. | Private shared preferences of the application. | Until the settings are changed. | No |
| Selected audio file | Playing the recording during the call. | Private application storage. | Until the audio file is replaced or cleared. | Yes |

## 🛠️ Development

### Requirements

- JDK 17.
- Android SDK with API 36 platform and build tools.
- Android SDK location declared in `local.properties` through the `sdk.dir` property.

### Setup

```bash
git clone https://github.com/hmlendea/auto-call-responder.git
cd auto-call-responder
```

Then create `local.properties` with the path to the local Android SDK:

```properties
sdk.dir=/path/to/Android/Sdk
```

### Build

```bash
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

### Run

Install the debug APK on a connected device or emulator and launch the application from the launcher:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Linting

```bash
./gradlew lint
```

### Test

```bash
./gradlew test
```

## 🩺 Troubleshooting

| Symptom | Probable Cause | Resolution |
|---------|----------------|------------|
| Calls are not answered | The application does not hold the required roles or permissions. | Assign the dialer and call-screening roles and grant the requested permissions. |
| Calls are not answered | The responder is disabled, or the caller does not match the active filter. | Enable the responder in the settings and check the Romanian number filter. |
| Audio is not played | No audio file has been selected, or the selected file cannot be read. | Select the audio file again from the settings screen. |
| Audio stops early, and no notification arrives | This combination is not produced by the application: ending the call stops the audio and still sends the notification. | Confirm that the e-mail settings are complete; incomplete SMTP settings suppress the notification. |
| Audio is not audible to the caller | The uplink routing requires root access and a supported mixer control. | Confirm that root access is granted; without it, playback remains local to the device. |
| E-mails are not sent | The e-mail address or the SMTP credentials are incomplete or rejected. | Verify the address, host, port, user name, and password, then use the test e-mail action. |
| Handling is delayed | Battery optimisation suspends the background work. | Exempt the application from battery optimisation from the settings screen. |

## 🤝 Contributing

You are welcome to submit any suggestion, feedback, or modification to this project.

When doing so, please:
- Maintain cross-platform compatibility
- Submit focused pull requests that conform to the existing code style
- Maintain your branch synchronised with `master`
- Revise the documentation when functionality changes
- Raise a new [issue](https://github.com/hmlendea/auto-call-responder/issues) for problems or suggestions

## 💬 Support

General support inquiries and defect reports are handled through the [issue tracker](https://github.com/hmlendea/auto-call-responder/issues).

## 💝 Project Engagement

Discovered a problem or have a suggestion? [Open an issue](https://github.com/hmlendea/auto-call-responder/issues)!

If you find this project useful, consider starring ⭐️ it on GitHub!

## 📄 License

This project is being distributed under the `GNU General Public License v3.0` or later.
See [LICENSE](./LICENSE) for additional information.

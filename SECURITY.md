# Security Policy

Auto Call Responder is a device-local Android application that answers selected incoming calls, plays a local audio file during the handled call, and sends a notification e-mail through the SMTP server configured by the user. This policy defines the supported distribution channels, the private vulnerability-reporting process, the in-scope and out-of-scope report categories, and the coordinated disclosure expectations for the repository at https://github.com/hmlendea/auto-call-responder.

## 📑 Table of Contents

- [Supported Versions](#️-supported-versions)
- [Reporting a Vulnerability](#-reporting-a-vulnerability)
- [Scope](#-scope)
- [Disclosure Policy](#-disclosure-policy)
- [Safe Harbour](#-safe-harbour)
- [Recognition](#-recognition)

## 🛡️ Supported Versions

Use this table to indicate which project versions currently receive security maintenance.

| Version | Distribution Channel | Supported |
|---------|--------------------|-----------|
| Latest version | GitHub Releases | ✅ |
| Latest version | Source code (master branch) | ✅ |
| Latest version | Unofficial third-party distribution channels | ❌ |
| Preceding versions | Any distribution channel | ❌ |

## 🚨 Reporting a Vulnerability

Please do not disclose suspected vulnerabilities publicly before maintainers have had an opportunity to validate and remediate them.

To report a vulnerability:
- [GitHub Security Advisories](https://github.com/hmlendea/auto-call-responder/security/advisories)
- Contact the maintainers directly

## 📌 Scope

The original maintainers are not responsible for changes, modifications, or security issues introduced by downstream forks.

The subsequent report categories are in scope for this repository:
- Call observation and answering through `CallScreeningService`, `AutoCallInCallService`, `CallHandlingService`, and `PhoneStateReceiver`
- Romanian fixed-line and mobile number matching in `RomanianPhoneNumbers`
- Storage of the settings and of the SMTP password in `SettingsManager`
- SMTP session establishment and notification composition in `EmailSender` and `EmailNotifier`
- Audio-file import in `AudioFilePicker`, the exported `FileProvider`, and playback control in `CallAudioController`
- Root command execution in `RootShell`
- The application manifest, the permission declarations, and the exported application components

The subsequent categories are out of scope unless explicitly stated to the contrary:
- Vulnerabilities in the Android operating system, vendor firmware, or the Android Telecom framework
- Vulnerabilities in the root manager application or in the audio mixer exposed by the device kernel
- Vulnerabilities in the SMTP server, the mail provider, or the mail transport configured by the user
- Malicious or malformed third-party audio files supplied by the user
- Issues that require physical device access, an unlocked bootloader, or an already-compromised device
- Vulnerabilities introduced by downstream forks or by modifications made by third parties

## 📢 Disclosure Policy

This project follows coordinated disclosure:
1. Vulnerabilities are investigated privately.
2. A remediation plan is prepared and validated.
3. Public disclosure is published after a fix, mitigation, or agreed risk decision is available.
4. Credit is attributed in accordance with reporter preference and project policy.

## 🧾 Safe Harbour

If your investigation is conducted in good faith, confined to authorised scope, and disclosed responsibly, the maintainers will not pursue action for policy-compliant activity.

## 🙏 Recognition

We appreciate responsible disclosure. Reporters who desire public attribution may be acknowledged in release notes, advisories, or a dedicated acknowledgements section.
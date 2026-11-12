# Privacy and Personal Data

Auto Call Responder is an Android application that runs entirely on the device of the person who installed it; it persists its configuration and the imported audio file in its private application storage and transmits data only to the SMTP server configured by the user.

## 📑 Table of Contents

- [What This Document Covers](#-what-this-document-covers)
- [Data We Handle](#-data-we-handle)
  - [Data Provided to the Application](#data-provided-to-the-application)
  - [Data Generated or Collected by the Application](#data-generated-or-collected-by-the-application)
  - [Data Received from Integrations](#data-received-from-integrations)
- [Processing and Use](#-processing-and-use)
- [Storage, Retention, and Deletion](#️-storage-retention-and-deletion)
- [External Processing and Integrations](#-external-processing-and-integrations)
- [User Controls and Requests](#️-user-controls-and-requests)
- [International Transfers](#-international-transfers)
- [Data Protection and Security](#️-data-protection-and-security)
- [Document Changes](#-document-changes)
- [Contact](#-contact)

## 🔎 What This Document Covers

This document describes how Auto Call Responder at https://github.com/hmlendea/auto-call-responder manages personal data. It covers the application conduct and verified integrations described below. Where the software is self-hosted, the instance operator may have separate responsibilities described below.

## 📥 Data We Handle

### Data Provided to the Application

- Configuration entered by the user in the settings screen: answering delay, internal delay, Romanian number filter, earpiece-only playback, recipient e-mail address, and SMTP host, port, user name, and password.
- The audio file selected by the user, which is copied into the private storage of the application.

### Data Generated or Collected by the Application

- Call metadata held in memory for the duration of a handled call: the caller number and the ring, answer, and end timestamps.
- No analytics, telemetry, crash reporting, or update checks are performed, and no personal data is generated or collected automatically beyond the in-memory call metadata.

### Data Received from Integrations

- No personal data is received from integrations or third parties. The caller number and the call state are obtained from the telephony framework of the operating system.

## 🧭 Processing and Use

The application processes the data described above for these verified functions:
- Answering incoming calls after the configured delay — answering delay and Romanian number filter settings.
- Playing the selected audio during the handled call — selected audio file and earpiece-only setting.
- Composing and sending the call notification e-mail — caller number, call timestamps, effective settings, recipient e-mail address, and SMTP credentials.

## 🗄️ Storage, Retention, and Deletion

The configuration is serialised as JSON under the `settings` key, and the imported audio URI under the `audio_file_uri` key, in the private shared preferences file `AutoCallResponderPrefs`. The imported audio file is stored as a single file inside `filesDir/call_audio`; the directory is purged on each import, so exactly one file is retained. Call metadata lives in memory for one call only and is never persisted. The cached root-availability flag is held in memory for the lifetime of the process.

The values are retained until they are changed in the settings screen, until the audio file is replaced or cleared, or until the application data is cleared or the application is uninstalled. The SMTP password is stored in the private shared preferences of the application, which are not encrypted.

## 🔗 External Processing and Integrations

The application contacts only the SMTP server configured by the user; no other external data transfer is built in.

| Service or integration | Purpose | Data involved | Configuration or documentation |
|-----------------------|---------|---------------|--------------------------------|
| SMTP server | Sending the call notification e-mail after a handled call ends. | Caller number, call timestamps, effective settings, and the sender and recipient e-mail addresses. | Configured in the application settings; the host, port, user name, and password are entered by the user. |

## ⚙️ User Controls and Requests

The following documented controls or request procedures are available:
- Enable or disable the responder — the main switch in the application.
- Change or clear the e-mail and SMTP settings — the settings screen.
- Replace or clear the imported audio file — the settings screen.
- Delete all stored data — clear the application data or uninstall the application.
- Report a data question or request — the [issue tracker](https://github.com/hmlendea/auto-call-responder/issues).

## 🌍 International Transfers

The application does not send data to a location controlled by the project maintainers. Data leaves the device only towards the SMTP server configured by the user, so the destination country or region is determined by that server and by the provider that hosts it.

## 🛡️ Data Protection and Security

The notification e-mail is sent over STARTTLS, or over implicit TLS on port 465, and the SMTP session is authenticated with `AUTH LOGIN`. The configuration and the imported audio file are stored in private application storage, which is not accessible to other applications, and call metadata is kept in memory only. The SMTP password is stored in the private shared preferences of the application, which are not encrypted, so the device screen lock and the overall security of the device remain relevant.

The original maintainers are not responsible for changes, modifications, or privacy issues introduced by downstream forks.

## 🔄 Document Changes

Update this document when application data processes, storage, integrations, or deployment responsibilities change. The current version is published at https://github.com/hmlendea/auto-call-responder/blob/master/PRIVACY.md.

## 📬 Contact

For questions about application data handling, contact the project maintainers. For a self-hosted instance, contact the instance operator, unless the project explicitly manages the request. Include the application version and the Android version of the device; do not send passwords, access tokens, or other secrets.

The project maintainers can be reached through the [issue tracker](https://github.com/hmlendea/auto-call-responder/issues).

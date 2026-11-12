# API usage examples

The application exposes no public HTTP API. This document provides concrete examples of the Android contracts it implements, using the exact component names, actions, extras, and identifiers declared in the manifest and the source.

## Start the legacy call-handling service

`PhoneStateReceiver.startCallHandling` constructs this intent. The action and extra are the only inputs the service consumes on the `ACTION_START` path.

```kotlin
val intent = Intent(context, CallHandlingService::class.java).apply {
    action = CallHandlingService.ACTION_START          // "ACTION_START"
    putExtra(CallHandlingService.EXTRA_CALLER_NUMBER, callerNumber)
}
context.startForegroundService(intent)
```

## Stop the legacy call-handling service when the call ends

`PhoneStateReceiver.stopCallHandling` forwards the caller number so a call that ended during the pre-answer delay is still reported.

```kotlin
val intent = Intent(context, CallHandlingService::class.java).apply {
    action = CallHandlingService.ACTION_STOP           // "ACTION_STOP"
    putExtra(CallHandlingService.EXTRA_CALLER_NUMBER, callerNumber)
}
context.startForegroundService(intent)
```

## Screening response

`CallScreeningService` always returns an empty response, which instructs Telecom to neither disallow, reject, nor silence the call.

```kotlin
val allowResponse = CallScreeningService.CallResponse.Builder().build()
respondToCall(callDetails, allowResponse)
```

## Answer and disconnect on the dialer path

```kotlin
call.answer(VideoProfile.STATE_AUDIO_ONLY)   // answer as a voice call
// …
call.disconnect()                            // end the call
```

## Mute and route on the dialer path

```kotlin
inCallService.setMuted(true)                                      // mute the microphone
inCallService.setAudioRoute(CallAudioState.ROUTE_EARPIECE)        // or ROUTE_SPEAKER
```

## Answer and end on the legacy path

```kotlin
@Suppress("DEPRECATION")
telecomManager.acceptRingingCall()
// …
telecomManager.endCall()   // requires API 28 or later and ANSWER_PHONE_CALLS
```

## Import an audio document

```kotlin
AudioFilePicker.launch(activity)                  // ACTION_GET_CONTENT → OPEN_DOCUMENT → PICK
// in onActivityResult, requestCode == AudioFilePicker.REQUEST_CODE (1001):
val importedUri = AudioFilePicker.import(context, sourceUri)
// → content://<applicationId>.fileprovider/call_audio/selected_call_audio.mp3
```

## Send a test e-mail

`MainActivity.sendTestEmailMessage` builds the transport directly.

```kotlin
EmailSender(
    host = settings.smtpHost,
    port = settings.smtpPort,
    username = settings.smtpUser,
    password = settings.smtpPassword
).send(
    senderAddress = settings.smtpUser,
    recipientAddress = settings.emailAddress,
    subject = "Test email from Auto Call Responder",
    body = body
)
```

## SMTP conversation

The exact command sequence performed by `SmtpConnection`.

```text
S: 220 <server greeting>
C: EHLO <local canonical host name>
S: 250 ...
C: STARTTLS            (omitted when the port is 465; implicit TLS is used instead)
S: 220
C: EHLO <local canonical host name>
S: 250 ...
C: AUTH LOGIN
S: 334 ...
C: <base64 user name>
S: 334 ...
C: <base64 password>
S: 235 ...
C: MAIL FROM:<senderAddress>
S: 250 ...
C: RCPT TO:<recipientAddress>
S: 250 ...
C: DATA
S: 354 ...
C: From / To / Subject / MIME-Version / Content-Type / Content-Transfer-Encoding headers
C: <base64 body, 76-character lines>
C: .
S: 250 ...
C: QUIT
S: 221 ...
```

## Read the settings record

```kotlin
val settings = SettingsManager.getSettings()   // JSON under key "settings", or defaults
settings.delaySeconds                          // 3 by default
settings.filterRomanianNumbers                 // true by default
```

## Match a Romanian number

```kotlin
RomanianPhoneNumbers.isRomanian("+40 (722) 512-537")   // true
RomanianPhoneNumbers.isRomanian("+37360000000")        // false
```

## Root command execution

```kotlin
RootShell.detectAsync { granted -> /* runs on the main thread */ }
RootShell.execute("cmd audio set-mic-mute true")   // returns output or null; 5-second timeout
```

## Related documents

- [Interface reference index](./api-reference/INDEX.md)
- [Integrations](./integrations.md)
- [Flow: notification delivery](./flows/notification-delivery.md)
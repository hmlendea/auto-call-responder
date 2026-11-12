# Notification component

## Components

| Class | File | Role |
|-------|------|------|
| `EmailNotifier` | `EmailNotifier.kt` | Compose and dispatch the call notification. |
| `EmailSender` | `EmailSender.kt` | Perform one SMTP transaction. |

## `EmailNotifier`

`object EmailNotifier` with tag `EmailNotifier`.

### `sendCallNotification(callerNumber, ringTimeMillis, answerTimeMillis, endTimeMillis)`

Builds the subject and body, then calls `send`.

### Subject

| Condition | Subject |
|-----------|---------|
| Blank caller number | `Handled an unknown call` |
| Otherwise | `Handled a call from <number>` |

### Body sections

| Section | Contents |
|---------|----------|
| Greeting | "Hello," and a statement that the application handled a call. |
| Call summary | Caller number (or "Unknown / private number"), first ring, answered at (or "not answered automatically"), call ended, answer delay, call duration after answer, total time. |
| Playback | Audio file description (settings path, audio URI, or "No audio file configured"), output route, and a note that Android cannot inject audio into the uplink. |
| Settings at the time of the call | Enabled, answering delay, internal delay, Romanian filter. |
| Device | Manufacturer, model, Android release, and SDK level. |
| Footer | "Sent automatically by Auto Call Responder." |

`durationSeconds` returns `0` when the start is non-positive or the end precedes the start. `formatTimestamp` returns `"unknown"` for non-positive values and otherwise formats `yyyy-MM-dd HH:mm:ss` in the default locale.

### Dispatch

`send` refuses to continue when `emailAddress`, `smtpUser`, or `smtpPassword` is blank, logging "Email settings are incomplete". Otherwise it launches an unbound `CoroutineScope(Dispatchers.IO)` that constructs `EmailSender`, sends, and logs success or failure. The caller never observes the outcome.

## `EmailSender`

`class EmailSender(host, port, username, password)`.

### `send(senderAddress, recipientAddress, subject, body)`

1. Open the connection with implicit TLS when `port == 465`.
2. For any other port, issue `STARTTLS`.
3. Authenticate with `AUTH LOGIN` when `username` is non-blank.
4. Send `MAIL FROM`, `RCPT TO`, and `DATA`.
5. `QUIT`, then close in `finally`.

### `SmtpConnection` (private)

| Aspect | Value |
|--------|-------|
| Connect timeout | 20 000 ms |
| Read timeout | 20 000 ms |
| Line terminator | CRLF |
| Body encoding | Base64, UTF-8, 76-character lines |
| MIME | `text/plain; charset=UTF-8`, `Content-Transfer-Encoding: base64` |
| Subject encoding | RFC 2047 base64 when the subject contains non-printable ASCII |
| Local identity | `InetAddress.getLocalHost().canonicalHostName`, falling back to `localhost` |
| Expected statuses | `220`, `250`, `354`, `221`, `334`, `235` |

`expect` skips multi-line responses (a `-` at index 3) until a final line, then requires the expected status prefix. Any mismatch or a closed connection raises `IOException`.

## Guarantees

- The notification is sent only after the call has been disconnected.
- Exactly one notification is sent per call, enforced by the call handlers.
- No retry, queue, or durable outbox exists.
- The password is never logged.

## Related documents

- [Flow: notification delivery](../flows/notification-delivery.md)
- [Behaviour: call notification e-mails](../behaviour/call-notification-emails.md)
- [Integrations](../integrations.md)
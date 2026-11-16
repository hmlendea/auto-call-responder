# Notification component

## Components

| Class | File | Role |
|-------|------|------|
| `EmailNotifier` | `EmailNotifier.kt` | Resolve the caller contact, compose the call notification, and dispatch it. |
| `ContactResolver` | `ContactResolver.kt` | Look up the caller number in the contacts list. |
| `EmailSender` | `EmailSender.kt` | Perform one SMTP transaction. |

## `EmailNotifier`

`object EmailNotifier` with tag `EmailNotifier`.

### `sendCallNotification(context, callerNumber, ringTimeMillis, answerTimeMillis, endTimeMillis)`

Resolves the caller contact and builds the subject and body on the IO dispatcher, then calls `send`.

### Subject

| Condition | Subject |
|-----------|---------|
| Blank caller number | `Handled an unknown call` |
| Number not in the contacts list | `Handled a call from <number>` |
| Number in the contacts list | `Handled a call from <contact name> (<number>)` |

### Body sections

| Section | Contents |
|---------|----------|
| Greeting | "Hello," and a statement that the application handled a call. |
| Call summary | Caller number (or "Unknown / private number"), contact name (or "Not in contacts"), number type (or "Unknown"), first ring, answered at (or "not answered automatically"), call ended, answer delay, call duration after answer, total time. |
| Playback | Audio file description (settings path, audio URI, or "No audio file configured"), output route, and a note that Android cannot inject audio into the uplink. |
| Settings at the time of the call | Enabled, answering delay, internal delay, Romanian filter. |
| Device | Manufacturer, model, Android release, and SDK level. |
| Footer | "Sent automatically by Auto Call Responder." |

`durationSeconds` returns `0` when the start is non-positive or the end precedes the start. `formatTimestamp` returns `"unknown"` for non-positive values and otherwise formats `yyyy-MM-dd HH:mm:ss` in the default locale.

### Contact resolution

`ContactResolver.resolve(context, rawNumber)` returns a `CallerContact(displayName, numberType)` or `null`:

- the number is normalised (a `tel:` prefix and separators are removed) before the lookup;
- `ContactsContract.PhoneLookup.CONTENT_FILTER_URI` is queried with the display name, type, and label columns, and the first entry with a non-blank name is used;
- the type label is localised through `ContactsContract.CommonDataKinds.Phone.getTypeLabel`, so a custom label is used verbatim;
- `null` is returned when the number is blank, `READ_CONTACTS` is not granted, the permission is revoked while the call was in progress, the number is not stored, or the query throws; each case is logged.

The lookup runs inside the notification IO coroutine, so resolving the contact never delays the SMTP transaction or the caller.

### Dispatch

`send` refuses to continue when `emailAddress`, `smtpUser`, or `smtpPassword` is blank, logging "Email settings are incomplete". Otherwise it launches an unbound `CoroutineScope(Dispatchers.IO)` that resolves the contact, builds the subject and body, constructs `EmailSender`, sends, and logs success or failure. The caller never observes the outcome.

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

## `ContactResolver`

`object ContactResolver` with tag `ContactResolver` and the value type `CallerContact(displayName, numberType)`.

| Aspect | Value |
|--------|-------|
| Lookup URI | `ContactsContract.PhoneLookup.CONTENT_FILTER_URI` with the normalised number appended and URI-encoded |
| Projection | `DISPLAY_NAME`, `TYPE`, `LABEL` |
| Type label | `Phone.getTypeLabel(resources, type, label)`, falling back to the custom label when the type is absent |
| Best effort | Returns `null` on a missing or revoked permission, a blank number, no match, or any exception |

## Guarantees

- The notification is sent only after the call has been disconnected.
- Exactly one notification is sent per call, enforced by the call handlers.
- No retry, queue, or durable outbox exists.
- The password is never logged.

## Related documents

- [Flow: notification delivery](../flows/notification-delivery.md)
- [Behaviour: call notification e-mails](../behaviour/call-notification-emails.md)
- [Integrations](../integrations.md)
# Behaviour: call notification e-mails

## When a notification is sent

| Situation | Notification |
|-----------|--------------|
| The call was answered and ended by the application | Yes, after the disconnect attempt. |
| The caller hung up during the ringing delay (dialer path) | Yes, with `answerTimeMillis = 0`. |
| The caller hung up during the ringing delay (legacy path) | Yes, with `answerTimeMillis = 0`; the receiver forwards the number through `ACTION_STOP`. |
| The responder was disabled | No; no handler was created. |
| The number failed the active filter | No; no handler was created. |
| The recipient, user, or password is blank | No; the notifier logs an incomplete configuration. |

## Message format

- **Transport:** plain text, UTF-8, base64-encoded body, `Content-Transfer-Encoding: base64`.
- **Subject:** `Handled a call from <contact name> (<number>)` when the number is in the contacts list, `Handled a call from <number>` otherwise, or `Handled an unknown call` when the number is blank.
- **Sender:** the configured SMTP user name.
- **Recipient:** the configured e-mail address.

## Content

| Section | Fields |
|---------|--------|
| Call summary | Caller number, contact name (or "Not in contacts"), number type (or "Unknown"), first ring, answered at (or "not answered automatically"), call ended, answer delay, call duration after answer, total time. |
| Playback | Audio file description, output route, and the uplink-injection note. |
| Settings at the time of the call | Enabled, answering delay, internal delay, Romanian filter. |
| Device | Manufacturer, model, Android release, SDK level. |

## Derived values

| Value | Formula | Zero condition |
|-------|---------|----------------|
| Answer delay | `answerTimeMillis - ringTimeMillis` | Start non-positive or end before start. |
| Call duration after answer | `endTimeMillis - answerTimeMillis` | Same. |
| Total time | `endTimeMillis - ringTimeMillis` | Same. |

When the call was not answered automatically, the answer delay and talk duration are omitted and the "answered at" line reads "not answered automatically".

## Contact resolution

`ContactResolver.resolve` is called inside the notification coroutine, before the message is built:

- the number is normalised (no `tel:` prefix, separators removed) and looked up through `ContactsContract.PhoneLookup.CONTENT_FILTER_URI`;
- the display name and the phone type label are returned; the label is produced by `ContactsContract.CommonDataKinds.Phone.getTypeLabel`, so mobile, work, and custom labels are localised, and a custom label is used verbatim;
- the lookup returns `null` when `READ_CONTACTS` is not granted, the number is not stored, the permission is revoked mid-run, or the query fails; the message then states "Not in contacts" and "Unknown".

The contacts list is never modified and no contact data is persisted.

## Privacy

The message contains the caller number, the caller contact name and number type when the number is stored in the contacts list, the call timestamps, the effective settings, and the device model. It is delivered to the recipient through the configured SMTP server, so the mail provider and any transport between the device and that server determine the final handling. The SMTP password is never included.

## Failure behaviour

Delivery is asynchronous and fire-and-forget. A failure is logged on the device and is invisible to the user during call handling; the **Test Email** action surfaces the failure in a toast. No retry or durable outbox exists.

## Related documents

- [Flow: notification delivery](../flows/notification-delivery.md)
- [Notifications component](../components/notifications.md)
- [Privacy and data](../security.md)
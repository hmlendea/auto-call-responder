# Interface reference index

## No public HTTP API

Auto Call Responder is a device-local Android application. It exposes **no** HTTP, gRPC, or network API. The only network interaction is an outbound SMTP transaction. This directory documents the Android platform interfaces the application implements or invokes, which are the closest equivalent to an API surface. The structure mirrors an API reference: one file per externally triggered entry point, with inputs, outputs, errors, and guarantees.

## Interface summary

| Interface | Kind | Contract | Entry point | Auth or binding | File |
|-----------|------|----------|-------------|-----------------|------|
| `android.telecom.CallScreeningService` | Inbound binding | Allow, reject, or silence a call | `onScreenCall` | `BIND_SCREENING_SERVICE`, call-screening role | [call-screening-service.md](./call-screening-service.md) |
| `android.telecom.InCallService` | Inbound binding | Own calls, mute, route | `onCallAdded`, `onCallRemoved`, `Call.Callback` | `BIND_INCALL_SERVICE`, dialer role | [in-call-service.md](./in-call-service.md) |
| `android.intent.action.PHONE_STATE` | Inbound broadcast | Report call state transitions | `onReceive` | — (exported receiver) | [phone-state-receiver.md](./phone-state-receiver.md) |
| Boot broadcasts | Inbound broadcast | Restore readiness | `onReceive` | — (exported receiver) | [boot-receiver.md](./boot-receiver.md) |
| `CallHandlingService` | Inbound service start | Start or stop legacy handling | `onStartCommand` | — (`exported="false"`) | [call-handling-service.md](./call-handling-service.md) |
| `TelecomManager` | Outbound API | Answer and end calls | `acceptRingingCall`, `endCall` | `ANSWER_PHONE_CALLS` | [in-call-service.md](./in-call-service.md) |
| `FileProvider` | Internal content provider | Serve the imported audio | Authority `${applicationId}.fileprovider` | `exported="false"` | [../integrations.md](../integrations.md) |

## Global conventions

| Aspect | Convention |
|--------|------------|
| Caller number normalisation | Remove a `tel:` prefix and whitespace; the filter strips separators. |
| Error reporting | Logged to logcat; the user-visible surface is toasts on the activities only. |
| Idempotency | The call handlers guarantee one answer and one notification per call. |
| Versioning | There is no versioned interface; the platform contract versions are the Android API levels. |
| Rate limiting | None; the platform serialises callbacks for a single call. |

## Authentication and authorisation

There is no user authentication. Authorisation is delegated to Android:

- the platform bind permissions gate the exported services;
- the dialer and call-screening roles gate the Telecom bindings;
- runtime permissions gate answering, ending, audio, and notifications;
- the root manager gates privileged commands.

## Related flows and components

- [Flow: dialer call handling](../flows/dialer-call-handling.md)
- [Flow: legacy call handling](../flows/legacy-call-handling.md)
- [Flow: notification delivery](../flows/notification-delivery.md)
- [Components: call observation](../components/call-observation.md)
- [Data model](../data-model.md)
- [API usage examples](../api-usage-examples.md)
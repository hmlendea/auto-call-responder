# CallScreeningService

Base contract: `android.telecom.CallScreeningService`, declared with the action `android.telecom.CallScreeningService` and the permission `android.permission.BIND_SCREENING_SERVICE`.

Implementation: `ro.nucilandia.autocallresponder.CallScreeningService`, file `CallScreeningService.kt`.

## onScreenCall(callDetails: Call.Details)

Invoked by Telecom once per incoming call while the application holds the call-screening role. Telecom unbinds the service as soon as the method returns.

### Request

| Input | Type | Source | Notes |
|-------|------|--------|-------|
| `callDetails` | `Call.Details` | Telecom | Carries the handle in `details.handle`. |

The caller number is derived as `details.handle?.schemeSpecificPart ?: details.handle?.toString() ?: ""`, with a `tel:` prefix removed and whitespace stripped.

### Response

The method calls `respondToCall(callDetails, ALLOW_RESPONSE)` where `ALLOW_RESPONSE` is an empty `CallResponse.Builder().build()`. An empty response means the call is neither disallowed, rejected, nor silenced, so it keeps ringing.

### Behaviour summary

| Step | Effect |
|------|--------|
| 1 | Always allow the call. |
| 2 | Read the settings; return when `enabled` is false. |
| 3 | Return when `filterRomanianNumbers` is true and the number is not Romanian. |
| 4 | Log that the call will be handled by `AutoCallInCallService`. |

Side effects: log entries only. The service never answers, plays audio, or persists anything.

### Guarantees

| Guarantee | Reason |
|-----------|--------|
| The call is never blocked or silenced by screening | The response is always the empty allow response. |
| Screening never delays the ringing | No blocking work is performed. |
| The decision matches the handling decision | The in-call service applies the same enabled-then-filter rule. |

### Error scenarios

| Scenario | Behaviour |
|----------|-----------|
| Handle unavailable | The number is empty; the filter rejects it when enabled. |
| Settings unreadable | Defaults are used. |
| An exception inside the method | Not caught, so it would propagate to Telecom; no known path raises one. |

### Flow references

- [Flow: dialer call handling](../flows/dialer-call-handling.md)
- [Behaviour: automatic call answering](../behaviour/automatic-call-answering.md)

### Related components

- `AutoCallInCallService` performs the actual handling.
- `RomanianPhoneNumbers` performs the filter.
- `SettingsManager` supplies `enabled` and `filterRomanianNumbers`.
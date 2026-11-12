# Documentation index

Master navigation for the Auto Call Responder knowledge base. Every document under `docs/` is indexed here with a one-line description.

## Repository summary

Auto Call Responder is a single-module Android application that answers eligible incoming calls automatically after a configurable delay, plays an imported audio file during the handled call, keeps the microphone muted outside that recording, ends the call after playback, and sends an owner e-mail describing the call once it has been disconnected. It observes calls through two mutually exclusive adapters — the role-bound Telecom services on Android 10 and later, and a legacy `PHONE_STATE` broadcast receiver — and owns all of its state in private application storage.

## Root document map

| Document | Purpose |
|----------|---------|
| [README.md](../README.md) | Installation, settings, permissions, privacy summary, and troubleshooting for users. |
| [ARCHITECTURE.md](../ARCHITECTURE.md) | Authoritative architecture: system context, components, runtime sequences, contracts, and design decisions. |
| [CONTRIBUTING.md](../CONTRIBUTING.md) | Issue, enhancement, and pull-request process for contributors. |
| [CODE_OF_CONDUCT.md](../CODE_OF_CONDUCT.md) | Conduct standards for all project spaces. |
| [SECURITY.md](../SECURITY.md) | Supported versions, vulnerability reporting, scope, and disclosure policy. |
| [PRIVACY.md](../PRIVACY.md) | Personal-data handling, storage, retention, and user controls. |
| [LICENSE](../LICENSE) | GNU General Public License v3.0 or later. |

## Start here

| Audience | Read |
|----------|------|
| New to the repository | [repository-overview.md](./repository-overview.md) then [quick-start.md](./quick-start.md) |
| New to the source tree | [repository-structure.md](./repository-structure.md) |
| Debugging a call that was not answered | [flows/dialer-call-handling.md](./flows/dialer-call-handling.md) and [troubleshooting.md](./troubleshooting.md) |
| Changing behaviour safely | [change-guide.md](./change-guide.md) and [invariants.md](./invariants.md) |
| Understanding why the design is as it is | [design-decisions.md](./design-decisions.md) and [ambiguities-and-open-questions.md](./ambiguities-and-open-questions.md) |

## Documentation catalogue

### Overview and navigation

| Document | Description |
|----------|-------------|
| [INDEX.md](./INDEX.md) | This master index. |
| [repository-overview.md](./repository-overview.md) | Repository purpose, scope, boundaries, and entry points. |
| [repository-structure.md](./repository-structure.md) | Source-tree layout and module organisation. |
| [quick-start.md](./quick-start.md) | Getting started guide for newcomers. |
| [faq.md](./faq.md) | Frequently asked questions. |
| [documentation-maintenance.md](./documentation-maintenance.md) | How to keep these documents current. |

### Architecture and design

| Document | Description |
|----------|-------------|
| [architecture.md](./architecture.md) | Deployment topology and boundary map that complements the root `ARCHITECTURE.md`. |
| [design-decisions.md](./design-decisions.md) | Key architectural choices and their rationale. |
| [invariants.md](./invariants.md) | System-wide invariants and contracts that must remain true. |
| [concurrency-and-scheduling.md](./concurrency-and-scheduling.md) | Threading, handlers, coroutines, and per-call scheduling. |
| [ambiguities-and-open-questions.md](./ambiguities-and-open-questions.md) | Unresolved behaviour, dead parameters, and known gaps. |

### Components and flows

| Document | Description |
|----------|-------------|
| [components/presentation.md](./components/presentation.md) | `MainActivity`, `SettingsActivity`, and `AudioFilePicker`. |
| [components/call-observation.md](./components/call-observation.md) | `CallScreeningService`, `AutoCallInCallService`, `PhoneStateReceiver`, and `BootReceiver`. |
| [components/call-orchestration.md](./components/call-orchestration.md) | `CallHandlingService`, call sessions, and the answering sequence. |
| [components/audio-control.md](./components/audio-control.md) | `CallAudioController`, microphone mute, route control, and uplink routing. |
| [components/root-integration.md](./components/root-integration.md) | `RootShell` detection and privileged command execution. |
| [components/settings-and-storage.md](./components/settings-and-storage.md) | `SettingsManager`, `AppSettings`, and the audio-file store. |
| [components/notifications.md](./components/notifications.md) | `EmailNotifier`, `EmailSender`, and the SMTP client. |
| [flows/startup-and-readiness.md](./flows/startup-and-readiness.md) | Process start, boot restoration, and readiness checks. |
| [flows/dialer-call-handling.md](./flows/dialer-call-handling.md) | End-to-end handling on the dialer path. |
| [flows/legacy-call-handling.md](./flows/legacy-call-handling.md) | End-to-end handling on the broadcast path. |
| [flows/notification-delivery.md](./flows/notification-delivery.md) | End-to-end notification e-mail delivery. |

### Behaviour

| Document | Description |
|----------|-------------|
| [behaviour/automatic-call-answering.md](./behaviour/automatic-call-answering.md) | Which calls are answered, when, and how they end. |
| [behaviour/audio-playback-and-mute.md](./behaviour/audio-playback-and-mute.md) | Playback, route selection, and the microphone-mute contract. |
| [behaviour/call-notification-emails.md](./behaviour/call-notification-emails.md) | What the notification e-mail contains and when it is sent. |

### Interface reference

| Document | Description |
|----------|-------------|
| [api-reference/INDEX.md](./api-reference/INDEX.md) | Interface reference index; no public HTTP API exists. |
| [api-reference/call-screening-service.md](./api-reference/call-screening-service.md) | Telecom call-screening contract. |
| [api-reference/in-call-service.md](./api-reference/in-call-service.md) | Telecom in-call contract and per-call callbacks. |
| [api-reference/phone-state-receiver.md](./api-reference/phone-state-receiver.md) | `PHONE_STATE` broadcast contract. |
| [api-reference/boot-receiver.md](./api-reference/boot-receiver.md) | Boot and package-replaced broadcast contract. |
| [api-reference/call-handling-service.md](./api-reference/call-handling-service.md) | Foreground-service action contract. |
| [api-usage-examples.md](./api-usage-examples.md) | Example intents, broadcasts, and SMTP sessions. |

### Platform and operations

| Document | Description |
|----------|-------------|
| [configuration.md](./configuration.md) | Settings schema, sources, precedence, and defaults. |
| [data-model.md](./data-model.md) | Domain objects, settings record, and in-memory call session. |
| [state-and-persistence.md](./state-and-persistence.md) | Stores, caches, backup rules, and lifecycle. |
| [integrations.md](./integrations.md) | Android Telecom, SMTP, root, and file-provider integrations. |
| [build-and-deployment.md](./build-and-deployment.md) | Build pipeline, artefact, CI, and installation. |
| [dependencies.md](./dependencies.md) | External and internal dependencies. |
| [security.md](./security.md) | Security model, threats, and mitigations (complements root `SECURITY.md`). |
| [logging.md](./logging.md) | Log tags, levels, sensitive values, and observability. |
| [error-handling.md](./error-handling.md) | Error taxonomy, degradation, and recovery. |
| [testing.md](./testing.md) | Test strategy, organisation, and coverage. |
| [troubleshooting.md](./troubleshooting.md) | Common issues and resolutions. |
| [change-guide.md](./change-guide.md) | How to modify common areas safely. |

## Coverage status

| Area | Coverage | Notes |
|------|----------|-------|
| Call observation and handling | Complete | Both adapters traced end to end. |
| Settings and persistence | Complete | Schema, default fallbacks, and backup rules documented. |
| Notification pipeline | Complete | SMTP conversation and message composition documented. |
| Root and audio routing | Complete | Detection, mixer catalogue, and degradation documented. |
| Automated tests | Complete | Single test class documented; no other automated coverage exists. |
| UI wiring | Complete | View identifiers, listeners, and lifecycle documented. |

Last reviewed: 2026-11-12. Owner: repository maintainers. Every statement in this catalogue is derived from the repository source at that revision.

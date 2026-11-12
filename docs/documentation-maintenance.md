# Documentation maintenance

## Ownership

The repository maintainers own this documentation. Contributors are expected to update the affected documents in the same pull request as the behaviour change, as stated in `CONTRIBUTING.md`.

## Trigger map

| Change | Documents to update |
|--------|---------------------|
| New or renamed setting | `docs/configuration.md`, `docs/data-model.md`, `README.md` settings table |
| New or changed permission or role | `README.md` permissions table, `docs/integrations.md`, `docs/security.md` |
| Changed answering sequence or timing | `docs/flows/*.md`, `docs/concurrency-and-scheduling.md`, `docs/invariants.md`, root `ARCHITECTURE.md` |
| Changed notification content or transport | `docs/behaviour/call-notification-emails.md`, `docs/data-model.md`, `docs/integrations.md` |
| New dependency or toolchain version | `docs/dependencies.md`, `docs/build-and-deployment.md`, `README.md` development section |
| New component or file | `docs/repository-structure.md`, `docs/components/*`, `docs/INDEX.md`, root `ARCHITECTURE.md` source map |
| Changed test coverage | `docs/testing.md` |
| New workflow | `README.md` GitHub Actions table, `docs/build-and-deployment.md` |
| Changed persistence or backup | `docs/state-and-persistence.md`, `docs/security.md`, `README.md` privacy section |

## Synchronisation rules

- `docs/` complements the root documents; it never duplicates them. If a fact belongs in `ARCHITECTURE.md`, put it there and link from `docs/`.
- Keep the root `README.md` `## 📚 Documentation` section in step with `docs/INDEX.md`; it is a concise index, not a copy.
- Relative links from `docs/` are written from the `docs/` root; links to root documents use `../`.
- Cross-references use relative paths only, so the tree stays portable.

## Conventions

| Aspect | Convention |
|--------|------------|
| File names | kebab-case, plural directory names for collections |
| Headings | Sentence case |
| Code symbols | Backticks with the class or method name |
| Tables | Used only for genuinely tabular information |
| Dates | ISO `YYYY-MM-DD` |

## Review cadence

- Review `docs/INDEX.md` whenever a document is added, renamed, or removed.
- Update the "Last reviewed" line in `docs/INDEX.md` after a substantive revision.
- Re-verify the source map in this tree after any file addition or rename, because it is the primary orientation aid for future contributors.

## Related documents

- [INDEX](./INDEX.md)
- [Change guide](./change-guide.md)
- Root [CONTRIBUTING.md](../CONTRIBUTING.md)
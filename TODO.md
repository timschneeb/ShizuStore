# ShizuAppStore Client TODO

Live backlog. See `HANDOFF.md` for current state and `PLAN.md` for the design.
`CLEANUP.md` records the executed cleanup and what it deliberately deferred.

Global gate: `./gradlew assembleDebug testDebugUnitTest ktlintCheck lintDebug`

## Cleanup follow-ups

- [ ] Merge the `ResolvedApp`/`AppDetails`/`AppSource` presentation shim into
      the catalog models so `CatalogUiMapper` is no longer needed.


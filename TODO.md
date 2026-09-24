# ShizuAppStore Client TODO

Live backlog. See `HANDOFF.md` for current state and `PLAN.md` for the design.
`CLEANUP.md` records the executed cleanup and what it deliberately deferred.

Global gate: `./gradlew assembleDebug testDebugUnitTest ktlintCheck lintDebug`

## Cleanup follow-ups

- [ ] Merge the `ResolvedApp`/`AppDetails`/`AppSource` presentation shim into
      the catalog models so `CatalogUiMapper` is no longer needed.

## Launcher icon

- [ ] Pick an `artwork/` variant and wire it into `res/`: vector drawables for
      background, foreground and monochrome, updated `mipmap-anydpi-v26` XMLs,
      regenerated legacy PNGs (48/72/96/144/192 plus the round variant), and
      deletion of the old PNG layers.

## Full description source

- [x] awesome-shizuku sometimes links entries straight at a localized README
      (e.g. `README_EN.md`) when the project landing page is non-English. The
      full description always shows the repo's default `README.md` instead.
      Basic check: when the main link (`apps.url`) contains `/README` and
      points at a markdown document, use that link as the markdown source.
      Lands in the server README pick (`AppEnricher` / `GitHubReleaseClient`),
      not in the client.

# AGENTS.md - ShizuAppStore client

Android client for the Shizu app store. A fork of AuroraDroid (Kotlin, Jetpack
Compose, Material 3) retargeted from the F-Droid multi-repo index to the
`ShizuAppStoreServer` `/v1/*` REST API. Keep `docs/HANDOFF.md` current in the same pass as code.

## Commands

Run from this directory:

```bash
./gradlew assembleDebug
```

`assembleDebug` is the default inner loop and the only check needed while
iterating. Run `./gradlew testDebugUnitTest` when the change touches logic that
has unit tests (or when adding tests).

Lint and style checks are slow and only run before a new release:

```bash
./gradlew ktlintCheck lintDebug
```

`./gradlew ktlintFormat` fixes import ordering and formatting; run it before
`ktlintCheck`, especially after large renames.

Done means `assembleDebug` is green, unit tests pass for any change with test
coverage, and `ktlintCheck`/`lintDebug` are clean before a commit.

After a client change, install the fresh debug build to the phone over adb:

```bash
adb connect 192.168.178.58:5555
adb -s 192.168.178.58:5555 install -r app/build/outputs/apk/debug/ShizuStore-1.5.0-debug.apk
```

Adjust the device address if the phone reports a different one.

Performance benchmarks are device-gated and not part of the inner loop:

```bash
./gradlew :app:generateReleaseBaselineProfile
./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest
```

The first regenerates `app/src/release/generated/baselineProfiles/` (keep it in
source); the second runs the frame-timing suite: startup, tab switch, details
open and vertical flings through the home and catalog lists. The connected task
installs `app/build/outputs/apk/benchmarkRelease/ShizuStore-1.5.0-benchmarkRelease.apk`
itself and uninstalls the app when it finishes (the APK is `-unsigned` only on
machines without `signing.properties`). Grant `POST_NOTIFICATIONS` and wake the
screen first; a dozing screen yields no frame stats.

Judge animation and scroll work on a release-like install, never the debug APK:
debug is unminified and JIT-only, so it stutters in ways the shipped build does
not, and baseline profiles cannot help a debuggable app. For animation QA run
`./gradlew assembleNonMinifiedRelease` and install
`app/build/outputs/apk/nonMinifiedRelease/ShizuStore-1.5.0-nonMinifiedRelease.apk`
(AOSP testkey).

Compose compiler skipping and stability reports are opt-in because writing them
slows every build: `./gradlew assembleDebug -PcomposeMetrics=true` writes
`app/build/compose-metrics/` and `app/build/compose-reports/` (add
`:app:compileDebugKotlin --rerun-tasks` when the task is up to date).

## Layout

Single `:app` module. Sources under `app/src/main/java/me/timschneeberger/shizustore/`:

- `data/api/` - server DTOs, enums, OkHttp API client, throttle.
- `data/room/` - Room entities, DAOs, converters.
- `data/sync/` - catalog sync engine, DTO mapping.
- `data/repository/` - Room query facades consumed by ViewModels.
- `data/installer/`, `data/helper/`, `data/work/`, `data/receiver/` - download
  and install pipeline, inherited from AuroraDroid.
- `compose/` - screens, navigation (Navigation 3), shared widgets, theme.
- `viewmodel/` - per-screen ViewModels (Paging 3, Room-backed).

Outside the app module, `baselineprofile/` is a device-gated `com.android.test`
module with the Macrobenchmark suite and the baseline profile generator.

## Contract rules

- Server API is the source of truth: `../ShizuAppStoreServer/docs/SPEC.md` and
  `../ShizuAppStoreServer/src/ShizuAppStoreServer/Api/{Dtos,ApiEnums,AppMapper}.cs`.
  camelCase JSON; enums are lowercase/snake strings. Never guess a field name.
- Sync is `GET /v1/changes?since=` incremental; never a full dump. The cursor is
  the response's `generatedAt` (captured before its reads) minus a small safety
  margin, never a client clock; `/v1/meta` is only the fallback for older
  servers, and bootstrap captures meta before paging. Summaries in `/v1/changes`
  and `/v1/apps` carry no `downloads[]`; fetch `/v1/apps/{slug}` for install
  candidates. Rows awaiting their first successful check are never sent, so a new
  app appears only complete. A `catalogPurgeRequestedAt` newer than the locally
  applied marker wipes the cached catalog and re-bootstraps; the marker lives in
  DataStore and user data is never purged.
- Availability: `direct_apk` installs a matching candidate from `downloads[]`;
  `play_redirect` opens `storeUrl`; `link_only` opens `url`/`sourceUrl` in a
  Custom Tab; `excluded` is never sent. APKs are never mirrored: download the
  upstream `apkUrl`, extracting `archiveEntry` when it is a zip.
- Signatures: compute the installed cert SHA-256 and MD5; match against the
  space-joined `sigSha256`/`sigMd5` sets in `downloads[]`. Offer only the
  matching candidate and never move a user between signing keys. Match a
  candidate by membership in the set, not whole-string equality.
- Icons are immutable `{base}/icons/{iconHash}.png`; `iconAdaptive` selects
  rounded-square vs squircle framing; null `iconHash` uses the placeholder.
- Display behavior for app developers is documented in
  `../ShizuAppStoreServer/docs/listing-and-metadata.md`; update it when icon
  framing, availability actions, signature matching or list/detail fields change.

## Rules

- GPL-3.0-or-later. Keep upstream SPDX headers and `LICENSE`.
- Comments are concise and explain WHY, not WHAT. No em-dashes in code,
  comments, or docs.
- Tests are hermetic (MockWebServer for API/sync, in-memory Room/Robolectric for
  DAOs). Live-server tests are env-gated.
- Never commit unless asked. Update this file and `docs/HANDOFF.md` when component
  rules or state change.

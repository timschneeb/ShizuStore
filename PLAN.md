# ShizuAppStore Client Plan

Design of the Android client for the Shizu app store. A fork of AuroraDroid
(GPL-3.0-or-later, upstream `AuroraOSS/auroradroid`, HEAD `32385c8`, app version
2.0.1) retargeted from the F-Droid multi-repo index to the `ShizuAppStoreServer`
`/v1/*` REST API.

`HANDOFF.md` has the current state and gotchas; `TODO.md` is the live backlog.

## Locked decisions

- applicationId `me.timschneeberger.shizustore`, debug suffix `.debug`, nightly
  suffix `.nightly`.
- Full Kotlin package rename: `com.aurora.adroid` ->
  `me.timschneeberger.shizustore`, `com.aurora.extensions` ->
  `me.timschneeberger.shizustore.extensions`.
- Networking: raw OkHttp plus kotlinx.serialization, no Retrofit.
- Server: `BuildConfig.API_BASE_URL` is the production server
  (`https://shizustore.timschneeberger.me/`). Debug Settings -> Server offers a
  radio between Production and a custom URL persisted in DataStore; production is
  the default and the custom URL applies only while selected. LAN/emulator dev
  uses the custom option (`http://<pc-ip>:5137/`, or `http://10.0.2.2:5137/` from
  an emulator; the server must listen on `0.0.0.0`).
- DB `shizu.db`, DataStore `shizu_preferences`.

## Sources of truth

- Server behavior: `../ShizuAppStoreServer/docs/SPEC.md`.
- Server API: `../ShizuAppStoreServer/src/ShizuAppStoreServer/Api/{Dtos,ApiEnums,AppMapper}.cs`.
- Template: `../auroradroid/` (reference only, never edit).
- Workspace rules: root `AGENTS.md`.

## API layer (`data/api/`)

`ShizuDtos.kt` mirrors the server camelCase exactly. Dates stay raw ISO-8601
strings because the `since` cursor is replayed verbatim. Non-identity fields carry
defaults for tolerance. `ApiEnums.kt` maps `Availability`/`Listing`/`AppType`/
`CategorySection`/`SourceKind` with unknown values mapping to null rather than
throwing. `ShizuJson.kt` uses `ignoreUnknownKeys`, `coerceInputValues` and
`explicitNulls = false`.

`ShizuApi`/`OkHttpShizuApi` covers `apps`, `app`, `categories`, `changes`, `meta`,
`reportInstall` and `health`. `ApiResult`/`ApiError` are sealed
(`Network`, `Http`, `Parse`, `RateLimited`, `NotConfigured`); `EtagResult` keeps
304 and 404 distinguishable for the ETag endpoints. `BaseUrlProvider` resolves the stored custom URL over the production default when
the custom option is selected. `ShizuUrls.icon(base, hash)` builds the immutable
icon URL.

`RequestThrottle` serializes requests with a 650ms minimum spacing under the
server's 100 req/min/IP cap and doubles a 429 backoff from 5s to 60s (the server
does not guarantee `Retry-After`). Clock and sleep are injectable for tests.

## Domain models (`data/model/`)

- `CertFingerprint(sha256, md5)` with `parseFingerprintSet` for the space-joined
  server sets, plus `fingerprintMatches`/`signaturesMatch` for set membership.
- `AppCandidate` built from an `AppDownloadEntity`: `source`, `apkUrl`,
  `archiveEntry`, `version`, `size`, `sha256`, signature sets, `minSdk`, `primary`,
  `matchesInstalled` and `isNewerThan`.
- `DetailedApp`: `app` plus `candidates`, `primaryCandidate`, `candidateFor`.
- `AppListArgs(query, categorySlug, sort, price, recommended)` drives the shared
  app list. `AppSort` carries the sort presets.

## Room (`data/room/`)

`ShizuStoreDatabase` is at version 1 with no migrations (fresh start pre-release).

- `AppEntity` (PK `slug`): summary, detail, denormalized update state
  (`updateAvailable`, `updateCandidateId`, `installedVersionCode`), popularity
  (`stars`, `downloadTotal`, `installCount`) and the ordering clocks
  (`versionUpdatedAt`, `listUpdatedAt`). `mergeDetailFrom` keeps detail columns on
  summary upserts.
- `AppDownloadEntity`: unique `(appSlug, sigKey)`, FK cascade to `app`; `sigKey` is
  the first space-token of `sigSha256`, else `sigMd5`, else `url:<apkUrl>`.
- `CategoryEntity`: the server tree flattened to `(slug, name, section, parentSlug,
  appCount, sortOrder)`.
- `SyncStateEntity` (singleton id 0): `cursor`, `categoriesEtag`, `listCommit`,
  `syncedAt` and the popularity flag.
- Catalog-scoped `FavouriteEntity`/`BlacklistEntity`/`IgnoredUpdateEntity` keyed by
  slug, so `link_only` apps with a null `packageName` work. `InstalledEntity`
  carries `signerMd5`.

`AppDao` exposes paging plus one `@RawQuery` (`AppListQueryBuilder`) for every
filter combination: bound WHERE clauses (recursive category subtree, escaped LIKE
search, price bucket, recommended) and a whitelisted ORDER BY. Listing is built in
Kotlin, not SQL, since the catalog is small.

## Sync (`data/sync/`, `data/helper/`, `data/work/`)

`CatalogSyncer` (single-flight via `Mutex`) bootstraps by paging, then applies
incrementals, as described in `HANDOFF.md`. Failures are `CatalogSyncFailure`
(network, http, parse, rate-limited). `CatalogMappers` converts DTOs to entities.
`DetailedAppRepository.fetchAndPersist(slug)` merges detail onto the summary and
replaces the candidate rows atomically.

`SyncHelper` is the global catalog control (WorkManager flow plus `sync()` and
`refresh()`); `SyncWorker` runs `sync-catalog`; `UpdateWorker` runs the periodic
sweep and posts the updates notification, and enqueues unattended installs when the
preference is set and the selected installer can install without confirmation.

## Install and download (`data/download/`, `data/helper/`)

`Download` is the queue row, staged by `DownloadHelper.stageRow(app, candidate)` or
`enqueueAndInstall(app, candidate)`. `DownloadWorker` fetches the single `apkUrl`,
verifies against the candidate hash (`ApkVerifier`, sha256 by default, md5 when the
server says so), extracts `archiveEntry` with `ArchiveExtractor` when present, and
cleans up on failure or cancel. `SourceLauncher` routes Play listings and
link-only pages to a Custom Tab.

A self-update is a normal install through the same pipeline: the store hides its
own catalog row (`SHIZU_STORE_PACKAGE`) from browse/search and the carousels but
keeps it in Installed/Updates, and settles the row from
`ACTION_MY_PACKAGE_REPLACED` after the replacement restarts the process.

## UI (`compose/`, `viewmodel/`)

Surfaces are kept from AuroraDroid: Downloads, Installed, Favourites, Blacklist,
IgnoredUpdates, Updates (plus the update sheet), Details, Settings, About.
`MainScreen` has three tabs (Apps, Search, Updates); the Search tab is one shared
`AppListScreen`/`AppListViewModel` used by search, category, recently added/updated
and the filtered lists, with filter chips for Category, Sort, Price and
Recommended. `AppDetailsScreen` shows candidates, availability actions and a
signature-match emphasis, and links the developer, permissions and related apps.
Icons go through `ShizuUrls.icon` and Coil with the `iconAdaptive` framing.

Settings adds a debug-only "Server" screen for the base URL override, validated
against `/healthz`.

## Tests

Hermetic, under `app/src/test/`: MockWebServer for API and sync, in-memory
Room/Robolectric for DAOs, temp folders for the hasher and archive extractor.
Coverage is focused on first-party logic: wire-enum parsing, throttle backoff,
signature matching, availability routing, catalog sync, update-state recompute,
the query builder, the markdown/HTML helpers and the install reporter. Live-server
tests are env-gated.

Gate: `./gradlew assembleDebug testDebugUnitTest ktlintCheck lintDebug`.

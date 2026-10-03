# ShizuAppStore client - HANDOFF

Fresh-agent context. Read `AGENTS.md` (rules and commands). This file records decisions, architecture and the state a new agent needs; release history lives in the workspace `../CHANGELOG.md`.

## What this is

Android client for the Shizu app store: a sideloaded store for Shizuku apps. A
fork of AuroraDroid (upstream `AuroraOSS/auroradroid`, HEAD `32385c8`, app version
2.0.1, GPL-3.0-or-later) retargeted from the F-Droid multi-repo index to the
`ShizuAppStoreServer` `/v1/*` REST API.

The server ingests the awesome-shizuku list and serves a catalog. APKs are never
mirrored: the client downloads from the upstream `apkUrl`. The inherited
browse/search/details UI, the `IInstaller` session/native/root/Shizuku flows and
signature display are kept. F-Droid index sync and multi-repo management are gone.

## Locked decisions

- applicationId `me.timschneeberger.shizustore`; debug suffix `.debug`, nightly
  suffix `.nightly`. Display name "Shizu Store".
- Full Kotlin package rename `com.aurora.adroid` -> `me.timschneeberger.shizustore`,
  `com.aurora.extensions` -> `me.timschneeberger.shizustore.extensions`.
- Raw OkHttp + kotlinx.serialization. No Retrofit.
- Server selection (Settings -> Server, shown in all builds): a radio chooses the
  production server (`BuildConfig.API_BASE_URL`,
  `https://shizustore.timschneeberger.me/`) or a custom URL persisted in
  DataStore. Production is the default and the custom URL applies only while its
  radio is selected. LAN/emulator dev uses the custom option
  (`http://<pc-ip>:5137/`, or `http://10.0.2.2:5137/` from an emulator; the dev
  server binds localhost by default, so it must listen on `0.0.0.0`). The same
  screen has a "Clear local database" action (confirm dialog) that wipes the
  cached catalog and cursor, then pulls the whole catalog from the active server.
  Below "Active server" the screen shows a Server info row once the `/v1/meta`
  probe succeeds: the server's app and category counts plus the short
  awesome-shizuku list commit. The commit line is omitted while a sync run is
  in progress (the API reports a null commit for the newest running run), and
  the whole row stays hidden until the probe succeeds.
- `rootProject.name = "ShizuAppStore"`. DB file `shizu.db`; DataStore store
  `shizu_preferences`. Versions: `versionName 1.4.1`, `versionCode 141`.
- Navigation 3 with a bottom nav of Apps / Search / Updates, Material 3 with
  dynamic colour, Paging 3, Hilt, WorkManager, Room, Coil.
- Self-update: the published store entry (`SHIZU_STORE_PACKAGE`,
  `me.timschneeberger.shizustore`) is hidden from browse/search and the home
  carousels on every variant, but stays in Installed and Updates. Updates are
  manual by default; unattended background updates follow the existing Settings
  toggle. No custom relaunch: the next launch reconciles.
- Storefront deep links: `https://shizustore.com/apps/{slug}` (App Link,
  `autoVerify`) and `shizustore://apps/{slug}` open the app detail screen.
  `MainActivity` is `singleTop`; cold starts seed the back stack from the
  intent, warm links go through `ShizuNavDisplay(deepLinkTarget = ...)`.
  `DeepLinks.parseAppId` is the only parser; the custom scheme also accepts
  `?package={pkg}`, which wins over the path slug. `AppDetailsViewModel.load`
  resolves its argument as package or slug and keys favourites on the row's
  package. The fingerprint published in the storefront's `assetlinks.json` is
  the release cert only, so debug builds test via the custom scheme. The
  details share action copies the storefront app page link.
- Android TV: `MainActivity` also carries `LEANBACK_LAUNCHER`, and the
  application ships `@drawable/tv_banner` (320x180 xhdpi, from
  `artwork/androidtv/`); `android.software.leanback` and
  `android.hardware.touchscreen` are `required="false"` so phone installs are
  unaffected. The phone Compose UI is what runs on TV.
- Remote and keyboard navigation: `Theme.kt` provides
  `LocalRippleThemeConfiguration = RippleDefaults.InsetFocusRingThemeConfiguration`
  so every ripple-based control gets a visible focus ring. `MainScreen` detects a
  remote or hardware keyboard (`Configuration.keyboard != KEYBOARD_NOKEYS` or TV
  `uiMode`) and focuses the current tab on launch; `HorizontalPager` uses
  `beyondViewportPageCount = 0` so an off-screen tab cannot swallow focus. The
  search field only force-opens the soft IME when no hardware keyboard is present.
  The screenshot viewer has focusable close/previous/next controls and starts on
  close; it also supports pinch zoom (up to 5x, one-finger pan) and pauses
   paging while zoomed. Pull-to-refresh still has no keyboard path
   (retry buttons cover error states).
- Motion: custom animation never hardcodes springs or tweens. `compose/theme/Motion.kt`
  exposes `motionSpatialSpec` / `motionFastSpatialSpec` / `motionEffectsSpec`, which
  read `MaterialTheme.motionScheme`, so bespoke motion stays in step with the
  MaterialExpressiveTheme components around it. Navigation 3 pushes/pops use
  defaultSpatial plus defaultEffects, the top bar title crossfades on tab change,
  `AnimatedAppIcon` springs its corner radius into a circle while installing, the
  updates, installed, downloads, favourites and ignored lists use
  `Modifier.animateItem` with scheme specs, the sources chevron and expanded rows
  share one vocabulary, the screenshot viewer springs a zoom back to rest on
  dismiss, and the pull-to-refresh placeholder crossfades in over loaded content.
  All specs are hoisted to composable scope because the animation lambdas that
  consume them are not composable. Settings -> Appearance has a Motion section
  with an Expressive motion switch (default on) that swaps the theme's
  `MotionScheme` between expressive and standard, calming both the M3 components
  and these specs when off. The shared-element icon transition suggested in
  `../IDEAS.md` is not implemented.

## Current state

Feature complete against the server `/v1` contract. `assembleDebug`,
`testDebugUnitTest`, `ktlintCheck` and `lintDebug` are green. Behavior worth
knowing before touching a subsystem:

- Detail screen: a changelog (forge markdown or F-Droid/Izzy HTML), a live
  README refetched on every details load, a screenshot gallery and an AI usage
  report (`usageShort`/`usageMarkdown` from Shizuku analysis) whose screen
  refetches once when an analyzable app has no report. Changelog, README and
  screenshots live only in bounded in-memory LRUs, never Room. Trackers, Dhizuku
  and localized labels come from server analysis fields; the list has no
  trackers badge (deliberate).
- Ignored updates: `ignored_update` writes a derived `AppEntity.updateIgnored`
  next to the truthful `updateAvailable`; updates lists, the updatable count and
  badges exclude ignored rows while the details Update button stays. A null
  `versionCode` ignores all future versions; a version-scoped ignore expires when
  a newer candidate appears and the stale row is pruned.
- Install reporting: the Settings -> Network switch gates install reports before
  the once-guard, so callbacks and `InstallReconciler` share one chokepoint; the
  body is `{"versionCode", "installType"}` and an empty body stays valid.
- Installer: a one-time `ShizukuPromptCard` on the Apps tab walks from "not
  installed" to "running and allowed" and sets
  `PREFERENCE_INSTALLER_ID = Installer.SHIZUKU` on grant; it is gated by
  `PREFERENCE_SHIZUKU_CARD_DISMISSED` (default false). Managers are found by
  permission across stock Shizuku, renamed forks and Sui. Settings -> App
  installer can override the installer source package (Shizuku only) and applies
  without restart.
- Catalog: candidates are keyed per ABI and per flavor `packageName`; primary
  selection prefers a compatible signer and ABI, and update-state resolution
  falls back from the canonical package to the installed flavor. The "Show
  closed-source apps" switch makes sync ask for `listing=main,closed_source`;
  turning it off drops cached `CLOSED_SOURCE` rows and re-bootstraps. Details
  show a forge source link normally but a `ClosedSourceNotice` for such rows.
- Home: one strip per top-level category with at least 4 apps, plus a synthetic
  "Dhizuku-compatible" section listing every `dhizukuDeclared` app. `POPULAR`
  ranks by `installCount`, hides itself, and its More page shows counts when the
  server reports `useInstallCountsForPopularity`.

## Architecture

**Sync.** `CatalogSyncer.clearCatalog()` wipes `app` (cascades `app_download`),
`category`, and `sync_state` under the sync mutex; `SyncHelper.clearLocalDatabase()`
runs it then `refresh()`, so the next sync bootstraps. `CatalogSyncer` is
single-flight. With no stored cursor it bootstraps by paging
`/v1/apps?sort=name&order=asc&pageSize=200` until `total` is reached, then prunes
slugs the server no longer has. In steady state it applies
`GET /v1/changes?since=<cursor>`: upserts added/updated summaries, deletes removed
rows (cascade to their candidates), and applies install-count deltas from
`installsUpdated` last via a narrow column write. A `catalogPurgeRequestedAt`
newer than the locally applied marker drops the cached catalog and bootstraps
instead of applying that payload. It refreshes `/v1/categories` when the stored
ETag changes. The cursor comes from the response's own `generatedAt`, which the
server captures before its reads, minus a one-second safety margin; bootstrap
captures `/v1/meta` before paging so rows committed mid-page replay through the
next delta. `meta().generatedAt` is only the fallback for older servers that omit
the delta cursor. The server hides rows until their first successful check, so a
newly listed app appears only complete (icon, description and download link
present). `/v1/changes` and `/v1/apps` carry summaries only, so
`DetailedAppRepository` fetches `/v1/apps/{slug}` for candidates, the full
description, the changelog and the screenshots. The README, its raw refetch URL,
the changelog and the screenshots are kept only in bounded in-memory LRUs, never
persisted to Room.

**Install.** `DownloadHelper` stages a queue row from a candidate. `DownloadWorker`
fetches the upstream `apkUrl`, verifies its hash, and extracts the named member
first when the release ships the APK inside an archive. `InstallWorker` hands the
file to the selected installer, then polls the row and the installed version until
it settles, so a dropped OEM broadcast cannot strand it. APKs are removed once the
package manager confirms the install, and `InstallReconciler` sweeps stranded rows
and files on launch, on foreground and after a settle timeout.

**Self-update.** `SHIZU_STORE_PACKAGE` is excluded from `pagedApps`
(`AppListQueryBuilder`) and every carousel in `AppRepository`, so the store
surfaces only via Installed and Updates. A self-replace kills the process, so
`SelfUpdateReceiver` (manifest, `ACTION_MY_PACKAGE_REPLACED`) settles the row and
mirrors the install on the next start; `InstallWorker` skips the dispatch when the
installed version already matches the row, so a rescheduled worker cannot loop the
update.

**Signatures.** The installed cert SHA-256 and MD5 are matched by membership
against the space-joined `sigSha256`/`sigMd5` sets in `downloads[]`
(`signaturesMatch`/`fingerprintMatches`), never by whole-string equality. Only a
matching candidate is offered, and a user is never moved between signing keys even
when a newer version exists under a different key. `UpdateStateRepository` pins
`updateCandidateId` to a matching candidate only.

**Availability.** `direct_apk` installs the matching candidate; `play_redirect`
opens `storeUrl`; `link_only` opens `url`/`sourceUrl` in a Custom Tab; `excluded` is
never sent. `SourceLauncher.sourceTarget` is the pure routing function.

**Icons.** Immutable `{base}/icons/{iconHash}.png`; `iconAdaptive` selects
rounded-square vs squircle framing; a null `iconHash` uses the placeholder. The
icon base is a process-wide snapshot (`ServerConfig`) kept fresh from
`BaseUrlProvider.observe()` so UI mapping stays synchronous. Coil's disk cache only
stores 2xx responses (`SuccessOnlyImageCacheStrategy`).

**API client.** `OkHttpShizuApi` marks every request `CacheControl.FORCE_NETWORK`
so the shared cache can never answer a `/v1/*` call (Room is the catalog cache).
The shared OkHttp client adds a Brotli interceptor (with a transparent gzip
fallback) and sends `User-Agent: Shizu Store/<version>`. `RequestThrottle`
serializes calls with a 650ms minimum spacing and doubles a 429 backoff from 5s to
60s; the server caps at 100 req/min/IP.

## Gotchas

- The Room database is `ShizuStoreDatabase` (schema `ShizuStoreDatabase`), now at
  version 10. Migrations `MIGRATION_1_2` through `MIGRATION_5_6` only add columns
  with defaults; `MIGRATION_6_7` swaps the marker classification for the AI usage
  report fields, `MIGRATION_7_8` adds `usageReportVersion`, `MIGRATION_8_9` drops
  the removed `blacklist` table and `MIGRATION_9_10` adds `app.updateIgnored`. A
  fresh install creates the full schema in one step and cached catalogs upgrade
  in place. Old `AuroraDatabase` migrations are gone; export schemas to
  `app/schemas/` as usual.
- When `candidate.archiveEntry` is set the server `sha256`/`size` describe the
  archive, not the APK. Verify the archive hash first, then extract the entry; both
  the `.apk` and the staged `.archive` are cleaned up on failure or cancel.
- Install completion cannot rely on the session broadcast alone: OEM freezers
  (observed on Samsung One UI) can drop it while the app is backgrounded, leaving
  the row `INSTALLING` until the next launch. `MainActivity.onStart` runs
  `reconcileOnForeground()`, `InstallWorker` polls `getDownload` plus
  `installedVersionCode` every second, and a landed version settles as `INSTALLED`
  even when this process dispatched the install.
- Update-state rewrites run `clearUpdateState()` plus every `setUpdateState()` in
  one `database.useWriterConnection { it.immediateTransaction { ... } }` so
  observers never see the cleared intermediate state. `withTransaction` needs a
  `SupportSQLiteOpenHelper`, which the bundled SQLite driver database lacks.
- The detail screen reads a nav key back to a slug through
  `AppRepository.getByPackage`, and `CatalogUiMapper` sets the presentation key to
  `packageName ?: slug`, so `link_only` apps (null `packageName`) still navigate.
- `SyncHelper.refresh()` marks a user refresh (`SyncStatusStore.manualRefreshing`,
  exposed as `SyncHelper.refreshing`); `sync()` is the silent background path.
  Screens drive the pull box and Updates header from `refreshing`, so the launch
  sync never blanks loaded content. `SyncHelper.syncing` counts only
  `WorkInfo.State.RUNNING`, so a retry backoff does not pin the indicator on.
  `clearLocalDatabase()` guards on connectivity first so an offline tap cannot
  leave the user with an empty store and no refill.
- `SyncWorker`/`UpdateWorker` return `Result.failure` on `CatalogSyncFailure.NETWORK`
  so failures settle instead of flapping on unbounded retry; only `RATE_LIMITED`
  retries.
- After any package/import mass rewrite, run `./gradlew ktlintFormat` before
  `ktlintCheck` (import ordering changes). `--nologo` is not a valid Gradle option
  here. `ktlintCheck` and `lintDebug` are clean.
- `auroradroid/` is reference only; never edit or bulk-copy it over this fork.

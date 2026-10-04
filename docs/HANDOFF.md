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
`uiMode`) and focuses the current tab on launch; tab content lives in an
`AnimatedContent` that composes only the outgoing and incoming tabs, so no
off-screen page can swallow focus. The
  search field only force-opens the soft IME when no hardware keyboard is present.
  The screenshot viewer has focusable close/previous/next controls and starts on
  close; it also supports pinch zoom (up to 5x, one-finger pan) and pauses
   paging while zoomed. Pull-to-refresh still has no keyboard path
   (retry buttons cover error states).
- Motion: custom animation never hardcodes springs or tweens. `compose/theme/Motion.kt`
  exposes `motionSpatialSpec` / `motionFastSpatialSpec` / `motionEffectsSpec`, which
  read `MaterialTheme.motionScheme`, so bespoke motion stays in step with the
  MaterialExpressiveTheme components around it. Navigation 3 pushes/pops use
  defaultSpatial plus defaultEffects, tab changes slide directly between the two
  selected tabs on the spatial spec (the middle tab never renders, and the
  Appearance toggle makes the slide expressive). All three tabs share one top
  bar that stays fixed above the animated content; on Search its title slot
  swaps to the search field and the action icons fade out, so the scaffold
  padding never changes between tabs and the slide stays purely horizontal,
  `AnimatedAppIcon` springs its corner radius into a circle while installing, the
  updates, installed, downloads, favourites and ignored lists use
  `Modifier.animateItem` with scheme specs, the sources chevron and expanded rows
  share one vocabulary, the screenshot viewer springs a zoom back to rest on
  dismiss, and the pull-to-refresh placeholder crossfades in over loaded content.
  All specs are hoisted to composable scope because the animation lambdas that
  consume them are not composable. Compose honors those springs only on
  uninterrupted enter/exit segments; a flip-back or an interrupted pop falls
  back to a hardcoded, critically damped spring. `MotionFallbackPatch` swaps
  that fallback for the active scheme's spring on startup and whenever the
  Appearance switch changes, so interrupted slides stay expressive too; if a
  future Compose renames the internal field the patch logs once and the app
  keeps the framework behavior (`MotionFallbackPatchTest` fails the build
  first). Settings -> Appearance has a Motion section
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
  refetches once when an analyzable app has no report. Changelog, changelog
  URL, README URL, screenshots and the full description persist in the `app`
  row on every detail fetch (schema v11, additive migration), so a warm reopen
  composes them on its first emission; the in-memory LRUs still back the
  fetch-time gate and the live README lookup. The screen only shows the
  spinner for rows this device has never fetched (summary rows lack
  url/storeUrl/permissions/candidates); already-fetched rows render
   immediately while the background fetch enriches. A "Statistics" section
   below the permissions/sources rows and above the author carousel draws
   charts from `GET /v1/apps/{slug}/history` (fetched once per load at 365
   days, off the loading path); it grows in with an expand+fade
   `AnimatedVisibility` when the fetch lands so the rows below slide
   instead of the block popping in. Icon-only segmented buttons switch between
    install activity (daily series across the whole window) and stars (new
    stars per week, derived client-side from the API's daily levels by
    differencing and Sunday-bucketing); the sparkline carries
    min/max y-axis labels in a left gutter, titles with `titleMedium` to
    match the other section headers, and captions the bucket size on the
    left ("Downloads per day" / "Stars per week") with the newest week's
    gain on the right. Dragging across the chart scrubs it: the nearest
    point gets a dot and a popover tooltip parked fully above the canvas
    (a finger on the line reaches downward, so nothing renders below the
    touch). The tooltip draws with an opaque elevated `surfaceContainerHigh`
    background, 12dp corners and a 16dp gap, floating over the rows above
     while shown, and shows the localized full date, the value with its
     bucket unit, and the cumulative total at that point. Scrub state lives
     in the chart child (series, values and totals are memoized in the
     body), so finger movement recomposes only that node; the gesture picks
     an axis at the touch slop, and a vertical drag releases the pointer to
     the page scroll while horizontal moves consume so the list never
     twitches under a scrub. Both modes wait for enough signal to draw (two
    recorded install days, two star levels) and the section renders nothing
    otherwise; the Settings -> Catalog "Show statistics" switch (default on)
    hides the section and skips the history fetch. The icon travels in
   the segmented button's label slot because M3 sizes that slot as the whole
   control, so an empty label would clip the icons. Trackers, Dhizuku
  and localized labels come from server analysis fields; the list has no
  trackers badge (deliberate). The author and category carousels at the very
  bottom subscribe eagerly instead of `WhileSubscribed`: their rows have to be
  measured before a fling reaches them, because height that only lands once the
  item first composes grows the list mid-fling and stops the scroll just short
  of the sections.
- Ignored updates: `ignored_update` writes a derived `AppEntity.updateIgnored`
  next to the truthful `updateAvailable`; updates lists, the updatable count and
  badges exclude ignored rows while the details Update button stays. A null
  `versionCode` ignores all future versions; a version-scoped ignore expires when
  a newer candidate appears and the stale row is pruned.
- Install reporting: the Settings -> Network switch gates install reports before
  the once-guard, so callbacks and `InstallReconciler` share one chokepoint; the
  body is `{"versionCode", "installType"}` and an empty body stays valid.
- Obtainium export: the Favourites and My apps top bars carry a tooltipped
  action that builds an Obtainium schema-v2 document and writes it through the
  storage access framework's create-document dialog, since Obtainium cannot
  ingest a config from a share target. Only rows with a package name and a forge
  or F-Droid source are included (`obtainiumRepoUrl`, so link-only and Play rows
  are skipped), and the builder omits `installedVersion` because Obtainium reads
  that from the device on import; empty exports and failed writes get toasts, and
  a successful save explains the Import/Export step.
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
- Home: curated strips first, then up to four use case strips with at least 4
  apps each, then one strip per top-level category with at least 4 apps, plus a
  synthetic "Dhizuku-compatible" section listing every `dhizukuDeclared` app.
  Use case strips come from the server's `/v1/use-cases` vocabulary (synced with
  its own ETag) and only list apps the classifier actually tagged; the More page
  browses that tag via the local `useCases LIKE` filter. `POPULAR`
  ranks by `installCount`, hides itself, and its More page shows counts when the
  server reports `useInstallCountsForPopularity`.
- Search home: below the category cloud the same use case vocabulary renders as
  chips (no filter-sheet entry and no detail chips by design); tapping one opens
  the list filtered by that use case.
- App list: sort chips show shortened labels while the bottom sheet shows the
  full ones (stars = "Most starred" vs "Most starred on GitHub/GitLab",
  downloads = "Total downloads" vs "Total downloads on ShizuStore"); sheet rows
  wrap long labels up to three lines (`AuroraListItem.headlineMaxLines`), the
  one-line chip keeps the short form. The
  `TRENDING` sort ("New installations in the last 14 days on ShizuStore
  (Trending)" in the sheet, "Trending" on the chip) orders by
  `AppEntity.trendScore`, a nullable column added in Room schema v12: picking
  the sort fetches `GET /v1/trending` (14 days, fresh installs only, top 100)
  through `TrendingRepository`, which clears and rewrites the scores in one
  transaction so apps that left the window drop back to the name tiebreak. Rows
  never fetched sort last; a failed fetch keeps the previous ranking. While the
  Trending sort is active each ranked row's meta line shows its window count
  behind a calendar-clock glyph and the download glyph, ahead of the version
  (`AppListItem.showTrend`); unranked rows show the version alone.
- Performance: the frame-timing suite has `scrollHome` and `scrollAppList`
  flings next to the startup, tab switch and details tests. The fling starts
  only after the skeleton is replaced, and the loaded probe searches descendant
  text nodes because Compose nests row text below the item node; the gesture is
  a timed `input swipe` inside the list bounds, since `UiObject2.fling` picked
  the full-screen pager. Baselines on SM-G998B (Android 14): startup 600-690ms
  TTI, scrollHome P50 6.6ms / P90 10.6ms, scrollAppList P50 7.2ms / P90 9.4ms,
  switchTabs P50 6.9ms, openAppDetails P50 10.4ms / P90 30.4ms / P95 45.9ms
  (frame overrun P90 +24.8ms, was +70.8ms). openAppDetails halved via two
  fixes: rows fetched before render immediately instead of waiting on the
  detail request, and fetch artifacts persist in Room (schema v11) so the
  fetch landing no longer swaps screenshots/changelog sections into the
  list mid-transition. The residual tail (one ~86ms frame per open) is the
  Navigation 3 transition finalizing content re-parenting; accept it unless
  measurements demand more. Debug builds stutter by design (no
  R8, JIT only), so animation QA installs `nonMinifiedRelease` or
  `benchmarkRelease`; `-PcomposeMetrics=true` writes Compose
  skipping/stability reports to `app/build/compose-metrics` and
  `app/build/compose-reports`.

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
description, the changelog and the screenshots. That fetch persists the
candidates plus the full description, changelog, screenshots and README URL
into the row (schema v11) and mirrors them into bounded in-memory LRUs, so a
warm reopen composes them from the first emission and the request settling
changes nothing visible.

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
  version 13. Migrations `MIGRATION_1_2` through `MIGRATION_5_6` only add columns
  with defaults; `MIGRATION_6_7` swaps the marker classification for the AI usage
  report fields, `MIGRATION_7_8` adds `usageReportVersion`, `MIGRATION_8_9` drops
  the removed `blacklist` table, `MIGRATION_9_10` adds `app.updateIgnored`,
  `MIGRATION_10_11` persists the changelog, screenshots and README fields on the
  `app` row, `MIGRATION_11_12` adds the nullable `app.trendScore` behind the
  Trending sort, and `MIGRATION_12_13` adds the `app.useCases` slug list, the
  `sync_state.useCasesEtag` ETag and the `use_case` vocabulary table behind the
  use case rows. A
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

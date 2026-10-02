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

## Current state

Feature complete against the server `/v1` contract. `assembleDebug`,
`testDebugUnitTest`, `ktlintCheck` and `lintDebug` are green. `TODO.md` lists the
known deviations. The entries below compact the September/October 2026 work.

### Details, deep links and metadata

- Deep links: the manifest declares the `shizustore.com` App Link (`pathPrefix
  /apps/`, `autoVerify`) plus `shizustore://apps/` on `singleTop` `MainActivity`,
  which also takes `?package={pkg}` (it wins over the slug).
  `DeepLinks.parseAppId` is the only parser; cold starts seed the back stack,
  warm links arrive in `ShizuNavDisplay`, which skips a push already on top.
  `AppDetailsViewModel.load` resolves slug or package and keys favourites on the
  row package; `assetlinks.json` carries the release cert only, so debug builds
  test via the custom scheme, and the share action copies the storefront link.
  Tests: `DeepLinksTest`.
- Changelog: detail-only `changelog` (forge release markdown, else F-Droid/Izzy
  long description) in a bounded `DetailedAppRepository.changelog` LRU, shown by
  `ChangelogScreen` through `MarkdownDescription`: markdown for forges,
  `AnnotatedString.fromHtml` for index HTML (`AppDetails.sourceKind`). The row
  appears only when non-blank. Tests: `DetailedAppRepositoryTest`,
  `MarkdownDescriptionTest.changelogRendersAsHtml`.
- Live README and screenshots: detail-only `readme_url` and `screenshots` in
  bounded LRUs; `LiveReadmeFetcher` (singleton OkHttp, `FORCE_NETWORK`, 2 MiB
  cap, fail-soft) refetches per details load and overrides the snapshot, and
  `ScreenshotGallery` renders a preview row plus fullscreen `HorizontalPager`.
  Tests: `LiveReadmeFetcherTest`, `DetailedAppRepositoryTest`.
- Open in browser: a Custom Tab action on the changelog screen
  (`changelog_url`, then `browserUrl`) and "More about" (`sourceUrl`, then
  `url`); hidden while loading or when no URL exists.
- Detail row icons: `SectionHeader` gained an optional 24dp primary icon; rows
  use `ic_info_outlined`, `ic_updates`, `ic_shield`, `ic_deployed_code_update`
  and the `LinkList` icons (`ic_code`, `ic_language`, `ic_storefront`). The
  legacy `issueTracker`/`translation`/`donate` fields and rows were removed.
- Relative ages: `RECENTLY_ADDED`/`RECENTLY_UPDATED` rows show the matching
  timestamp via `AppAge.LISTED`/`RELEASED`; `CatalogUiMapper` parses the ISO-8601
  strings once (`CommonUtil.parseIsoUtcMillis`, `time_*_ago` plurals). Core
  library desugaring covers `java.time` on old APIs. Tests: `CommonUtilTest`,
  `CatalogUiMapperTest`.
- Obtainium and Save APK: a top-bar Obtainium action for forge/F-Droid
  `sourceUrl`/`url` entries opens the Obtainium redirect deep link (`link_only`
  excluded); overflow "Save APK" reuses a staged download or
  `DownloadHelper.enqueue`, then `ApkSaver` on API 29+ or the SAF
  `CreateDocument` picker below. Every `AppExclusionMenu` item carries a leading
  icon via `MenuItemIcon`. Tests: `LinkListTest`.
- Ads and detail counts: `hasAds` drives the list "Ads" badge and
  `BillingNotice`; `DetailsStats` shows up to four cells (installs, downloads,
  stars, size) with tooltips, a 64dp scroll/wrap fallback and hidden for
  non-`direct_apk`. Chips show category (with `categoryIcon`), Android version,
  update age and license; the header is `versionName · repoName`. Tests:
  `CatalogUiMapperTest`.
- APK analysis signals: `dhizukuDeclared`, `trackers` and `trackerTags` (Room
  v2/v3) drive the Dhizuku chip and `TrackersNotice` plus its per-tracker dialog
  (category tags, Google search, code-signature caveat). The list has no
  trackers badge. Tests: `CatalogUiMapperTest`, `CatalogDaoTest`.
- APK facts and localized labels: server facts (`targetSdk`, `compileSdk`,
  `localeCount`, `abis`, `locales`, `localizedLabels`, `signerDn`,
  `signerScheme`, `signerKeyAlgorithm`) are stored (Room v4/v6) but only
  `localizedLabels` reaches the UI: `LocalizedLabel.pickLocalizedLabel` matches
  aapt2 qualifiers against device locales, list cards and `DetailsHeader` show
  the localized name, ordering keeps the catalog name. Signer facts stay
  display-only. Tests: `LocalizedLabelTest`, `CatalogDaoTest`,
  `CatalogUiMapperTest`.
- AI usage report: detail-only `usageShort`/`usageMarkdown` in `AppEntity` (Room
  v7 drops the old heuristic columns). `ShizukuUsageRow` opens
  `ShizukuUsageScreen`: hoisted markdown parse with one spinner, `MarkdownInline`
  summary card (no empty flash), primary bold, blockquote cards, trailing
  `> [!NOTE]` disclaimer. The ViewModel refetches once when an analyzable app
  has no report. Tests: `CatalogDaoTest.usageReportRoundTrip`,
  `CatalogUiMapperTest`, `DetailedAppRepositoryTest`.

### Install, updates and installer

- Stuck-install fix: OEM freezers (Samsung One UI) can drop the session
  broadcast while backgrounded. `InstallReconciler.reconcileOnForeground()` runs
  from `MainActivity.onStart`, `strandedInstallOutcome` settles a landed row even
  when this process dispatched it, and `InstallWorker.awaitSettled` polls the row
  and installed version every second. Tests: `InstallReconcilerTest`.
- Ignored updates: `ignored_update` writes a derived `AppEntity.updateIgnored`
  (Room v10) next to the truthful `updateAvailable`; paged updates,
  `observeUpdatableCount`, `AppDao.getUpdatable()`, `AppRepository.updatableApps()`
  and list badges exclude ignored rows while `ResolvedApp.hasManualUpdate` keeps
  the details Update button. Null `versionCode` ignores all future versions; a
  version-scoped ignore expires when a newer candidate appears and the stale row
  is pruned. Tests: `UpdateStateRepositoryTest`, `IgnoredUpdateRepositoryTest`,
  `CatalogDaoTest.updateStateDrivesUpdatableCount`.
- Blacklist removal: the client-only blacklist surface is gone (nothing enforced
  it); Room v9 `MIGRATION_8_9` drops the table. Favourites and ignored updates
  are the only preserved user lists.
- Self-update: the store package is hidden from browse/search and carousels on
  all variants (base id) but stays in Installed and Updates;
  `SelfUpdateReceiver` settles the self-replace. Tests:
  `AppListQueryBuilderTest`, `SelfUpdateGuardTest`.
- Install reporting: the Settings -> Network switch (`PREFERENCE_INSTALL_REPORTING`,
  on) is read inside `InstallReporter.reportInstalled` before the once-guard and
  the POST, so callbacks and `InstallReconciler` share one chokepoint. The POST
  body is `{"versionCode", "installType"}` (`fresh`/`update`/`unknown` from
  `PackageManager` timestamps via `resolveInstallType`; failures degrade to
  unknown) and `OkHttpShizuApi.post` stays compatible with an empty body.
  Tests: `InstallReporterTest`.
- Shizuku setup card: one-time `ShizukuPromptCard` on the Apps tab while Shizuku
  needs action (not installed -> Play listing; stopped -> open the permission-
  resolved manager, starting comes first; running but not allowed -> request the
  grant, which sets `PREFERENCE_INSTALLER_ID = Installer.SHIZUKU`). Hidden once
  installed, running and allowed; gated by
  `PREFERENCE_SHIZUKU_CARD_DISMISSED` (default false). Managers are found by
  `packageManager.getPermissionInfo` over
  `ShizukuInstaller.MANAGER_PERMISSIONS` (stock, then Shizuku+), so renamed forks
  count (Shizako included); `Sui.isSui()` is the embedded case. After a data reset
  `ShizuApp` picks
  `SHIZUKU` when available and allowed, else `SESSION`. State:
  `shizukuPrompt(available, running, permitted)`. Tests: `ShizukuPromptTest`,
  `ShizukuManagerPackagesTest`.
- Custom installer source: Settings -> App installer has a switch
  (`PREFERENCE_INSTALLER_CUSTOM_SOURCE`, off) plus a package field
  (`PREFERENCE_INSTALLER_CUSTOM_SOURCE_PACKAGE`, default `com.android.vending`),
  enabled only for the Shizuku installer. `effectiveInstallerSourcePackage` is
  the pure decision function and `ShizukuInstaller.resolveInstallerPackage`
  reads both preferences per install, so changes need no restart; bad names show
  an inline error. Test: `ShizukuInstallerSourceTest`.
- F-Droid download User-Agent: APK downloads send `User-Agent: <app>/<version>
  F-Droid` (some mirrors only serve the client agent). `OkHttpDownloader`
  appends its own interceptor so the API and Coil keep the plain agent; the
  server now publishes canonical upstream `apkUrl`s, so this only matters for
  details cached before that deploy.

### Catalog, sync and sources

- Per-architecture candidates (ABI): `DownloadDto.abi` folds into `sigKey`
  (lowercased suffix) so architectures sharing one signing key stay distinct.
  `AppCandidate.supportsAbi` accepts a null (universal) ABI or any token of
  `abiTokens` in `deviceAbis` (`Build.SUPPORTED_ABIS`, empty off-device);
  `abiTokens` splits the raw index `nativecode` on commas/whitespace,
  case-insensitively. `primaryCandidate`/`applyState` prefer a compatible
  candidate; `CatalogUiMapper.toSources` groups per-ABI rows by `(packageName,
  versionCode, signer)` and keeps the compatible one. Tests:
  `AppDownloadEntityTest`, `AppCandidateTest`, `CatalogUiMapperTest`.
- Per-flavor sources: downloads carrying `packageName` are separate source rows
  (unique `(appSlug, packageName, sigKey)`); `AppCandidate.from`,
  `Download.fromCatalog` and `candidateToResolvedApp` prefer the candidate
  package. `UpdateStateRepository` resolves the installed app against the
  canonical then candidate packages; `AppRepository.installedPackageFor` picks
  the installed flavor and `actionablePackage` carries it to Open, Uninstall,
  App info, the home shortcut and the Updates row; `getByPackage` falls back to
  `AppDao.getByDownloadPackage`. Tests: `UpdateStateRepositoryTest`,
  `DownloadFromCatalogTest`, `AppCandidateTest`, `CatalogDaoTest`.
- Closed-source handling: the "Show closed-source apps" switch
  (`PREFERENCE_SHOW_CLOSED_SOURCE`, off, in Settings -> Catalog) makes sync ask
  for `listing=main,closed_source` through `CatalogSyncer.listingParam` for
  bootstrap, `/v1/changes` and `/v1/categories`; opt-out deletes cached
  `CLOSED_SOURCE` rows and clears the cursor to re-bootstrap. Details keep a
  forge `sourceUrl` out of the Source code row (`isSourceCodeLink`) and show a
  `ClosedSourceNotice` card instead. The Catalog screen also holds "Show tracker
  info" (`PREFERENCE_SHOW_TRACKER_INFO`, on) which hides `TrackersNotice`.
  Tests: `CatalogSyncerTest`, `LinkListTest`, `CatalogUiMapperTest`.
- Remote catalog purge: `GET /v1/changes` carries `catalogPurgeRequestedAt`
  (server `config_flags` high-water mark). A value strictly newer than the
  DataStore `PREFERENCE_LAST_CATALOG_PURGE_AT` makes `CatalogSyncer` record the
  marker, drop `app`/`category`/`sync_state`, skip that delta and bootstrap in
  the same run; the category ETag is not reused after a purge. Favourites and
  ignored updates are never touched. Tests: `CatalogSyncerTest`.
- Home category sections: one `AppGroupKind.CATEGORY` strip per top-level
  category whose subtree (`CategoryTag.flatten()`) has at least 4 apps, ordered
  by member count then name, sorted by install count when the popularity flag is
  on else `downloadTotal`, capped at 20 (`CategorySections`). A synthetic
  "Dhizuku-compatible" section (slug `dhizuku`) lists every `dhizukuDeclared`
  app, filters by `dhizukuDeclared = 1` and is inserted into the filter sheet and
  tag cloud by `SyntheticCategory.insertDhizuku`. Tests:
  `CategorySectionsTest`, `AppListQueryBuilderTest`, `SyntheticCategoryTest`.
- Popular on ShizuStore: `AppGroupKind.POPULAR` between Most starred and Random
  picks, ranking by `installCount` (`AppDao.observePopular`, > 0, best first,
  limit 20, `hideSelf()`); `showInstalls` displays the counts. Its More page
  opens `AppSort.DOWNLOADS`, which shows counts when `/v1/meta` reports
  `useInstallCountsForPopularity` (on in production), hiding them for
  non-`direct_apk` rows. Test: `CatalogDaoTest`.

### Lists, search and navigation

- Search home and history: `AppListViewModel.atSearchHome` is an explicit place;
  clearing a filter keeps the list and only the back button (`SearchBackHandler`)
  returns to the tag cloud and recent searches. `setQuery` leaves home only for a
  non-blank query (the debounced observer fires right after the back button
  clears the field). `SearchHistoryStore` collapses prefix chains on record and
  read so recent searches no longer fill with `lin`, `link`, `linksheet`. Tests:
  `SearchHistoryStoreTest`.
- App list scroll restore: the popularity flag flow and the `(args, flag)`
  combine are `distinctUntilChanged()` so equal inputs never rebuild the Pager;
  `WindowAwarePagingSource` shifts the Room refresh key by the loaded window
  start; `AppListScreen` also records the first visible row (slug plus offset)
  and finds it by key on return (`ScrollAnchor`).
- Carousel refresh: `AppCarousel` keeps a list/grid state per strip and
  `scrollToItem(0)` from a `LaunchedEffect` keyed on the leading slug, so a
  changed ranking shows from the start; `AppsViewModel.retrySync()`
  re-randomizes the curated seeds on a user refresh, background syncs stay.
- Empty-state pull to refresh: `UpdatesScreen` wraps the whole `AnimatedContent`
  and uses a `fillParentMaxSize` placeholder inside a `LazyColumn` (a
  non-scrollable child swallows the nested-scroll gesture); the same applies to
  `AppListScreen` empty lists. `AppsScreen` keeps the limitation.

### Assets, localization and UI polish

- Usage screen: summary card text renders as markdown (`MarkdownInline`,
  synchronous) and the report parse state is hoisted
  (`rememberReportMarkdownState`, `RenderedMarkdown(state = ...)`) so one spinner
  covers the whole area. The details row and screen share
  `details_shizuku_title`.
- Icons: Material drawables were refreshed from two `new_icons/` batches;
  `Icons` usages outside `CategoryIcons.kt` became drawables and `FilterOption`
  gained an optional `iconRes`. Picks are deliberately not 1:1 (`ic_installed`
  draws install-done, `ic_group_network` is `network_manage`, `ic_cancel` is the
  circle-X); duplicates were deleted and call sites repointed. No SPDX headers
  on drawables (legacy Aurora GPL blocks aside). Launcher SVGs live in
  `artwork/` but none is wired in: the AuroraDroid PNGs still ship.
- Localization: `values-b+zh+Hans`, `values-b+zh+Hant` and `values-de-rDE` were
  added with AI assistance, then Crowdin sync took over (the base file is the
  only one hand-edited; a locale may lag). `generateLocaleConfig` plus
  `unqualifiedResLocale=en` derives the `LocaleConfig` at build time; no in-app
  picker, and `MissingTranslation` stays disabled, so a partial translation
  never fails the build.
- Store listing: four screenshots in
  `fastlane/metadata/android/en-US/images/phoneScreenshots/`, padded from
  1080x2325 to 1164x2325 for the 2:1 edge-ratio linter and embedded by the README.
- Donate and translate: the overflow `MoreSheet` carries Donate above About
  (`DonationDialog` with PayPal and Ko-fi, white-outlined PayPal mark for dark
  dialogs) and a "Help us translate" row opening
  `https://crowdin.com/project/shizustore` (`MoreSheet.CROWDIN_URL`). On demand
  only.

## Architecture

**Sync.** `CatalogSyncer.clearCatalog()` wipes `app` (cascades `app_download`),
`category`, and `sync_state` under the sync mutex; `SyncHelper.clearLocalDatabase()`
runs it then `refresh()`, so the next sync bootstraps. `CatalogSyncer` is
single-flight. With no stored cursor it bootstraps by
paging `/v1/apps?sort=name&order=asc&pageSize=200` until `total` is reached, then
prunes slugs the server no longer has. In steady state it applies
`GET /v1/changes?since=<cursor>`: upserts added/updated summaries, deletes removed
rows (cascade to their candidates), and applies install-count deltas from
`installsUpdated` last via a narrow column write. A `catalogPurgeRequestedAt`
newer than the locally applied marker drops the cached catalog and bootstraps
instead of applying that payload. It refreshes `/v1/categories`
when the stored ETag changes. The cursor comes from the response's own
`generatedAt`, which the server captures before its reads, minus a one-second
safety margin; bootstrap captures `/v1/meta` before paging so rows committed
mid-page replay through the next delta. `meta().generatedAt` is only the fallback
for older servers that omit the delta cursor. The server hides rows until their
first successful check, so a newly listed app appears only complete (icon,
description and download link present).
`/v1/changes` and `/v1/apps` carry summaries only, so `DetailedAppRepository`
fetches `/v1/apps/{slug}` for candidates, the full description, the changelog
and the screenshots. The README, its raw refetch URL, the changelog and the
screenshots are kept only in bounded in-memory LRUs, never persisted to Room.

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
  version 10. Migrations `MIGRATION_1_2` (analysis signals), `MIGRATION_2_3`
  (tracker tags), `MIGRATION_3_4` (badging and signer facts), `MIGRATION_4_5`
  (Shizuku usage classification) and `MIGRATION_5_6` (localized list names) only
  add columns with defaults; `MIGRATION_6_7` swaps the marker classification for
  the AI usage report fields, `MIGRATION_7_8` adds `usageReportVersion`,
  `MIGRATION_8_9` drops the removed `blacklist` table and `MIGRATION_9_10` adds
  `app.updateIgnored`. A fresh install creates the full schema in one step and
  cached catalogs upgrade in place. Old `AuroraDatabase` migrations are gone;
  export schemas to `app/schemas/` as usual.
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

# ShizuAppStore client - HANDOFF

Fresh-agent context. Read `AGENTS.md` (rules and commands) and `PLAN.md` (design)
first. `TODO.md` is the live backlog. This file records decisions, architecture and
the state a new agent needs.

## What this is

Android client for the Shizu app store: a sideloaded store for Shizuku apps. A
fork of AuroraDroid (upstream `AuroraOSS/auroradroid`, HEAD `32385c8`, app version
2.0.1, GPL-3.0-or-later) retargeted from the F-Droid multi-repo index to the
`ShizuAppStoreServer` `/v1/*` REST API. The pristine upstream template is at
`../auroradroid/` (reference only).

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
  `shizu_preferences`. Versions: `versionName 1.2.0`, `versionCode 120`.
- Navigation 3 with a bottom nav of Apps / Search / Updates, Material 3 with
  dynamic colour, Paging 3, Hilt, WorkManager, Room, Coil.
- Self-update: the published store entry (`SHIZU_STORE_PACKAGE`,
  `me.timschneeberger.shizustore`) is hidden from browse/search and the home
  carousels on every variant, but stays in Installed and Updates. Updates are
  manual by default; unattended background updates follow the existing Settings
  toggle. No custom relaunch: the next launch reconciles.

## Current state

Feature complete against the server `/v1` contract. `assembleDebug`,
`testDebugUnitTest`, `ktlintCheck` and `lintDebug` are green. See `TODO.md` for
the short list of known deviations. A September 2026 Compose performance pass
followed and is described below.

Release server override and F-Droid prep (September 2026): the Settings -> Server
screen and its navigation entry are no longer gated by `BuildConfig.DEBUG`, so
release builds can switch to a custom or self-hosted server too. Version bumped
to 1.0.1 (101), then 1.0.2 (102) for the signing block fix, then 1.0.3 (103) to
carry the reviewer-requested listing text in a reproducible release; 1.1.0 (110)
carries the app list scroll-restore fix and the new category icons; 1.2.0 (120)
carries the remote catalog purge, the Shizuku installer custom source setting,
Shizuku fork detection and the German/Chinese translations. 1.0.0 is
the first tagged release. Fastlane `short_description`
was cut to under 80 chars and `changelogs/100.txt`/`101.txt` were added. The
F-Droid metadata in `../fdroiddata/metadata/me.timschneeberger.shizustore.yml`
uses a reproducible build against the signed GitHub release APK (signing key
SHA-256 `e762ffc15d2f8de8ab191df53ea5a525f90d3ff37956fcd4e304e0340bafbaba`).
Local `apksigcopier` runs verified 1.0.0 and 1.0.1 byte-identical to the
published APKs modulo signature, including after the fdroidserver build steps.
The first F-Droid CI build failed its reproducibility check because the
buildserver has no NDK and AGP packaged the `libdatastore_shared_counter.so`
and `libsqliteJni.so` native libs unstripped; the build entry now pins
`ndk: 28.2.13676358` (AGP 9.3.2's default), which fdroidserver auto-installs
on the buildserver. F-Droid's `check apk` job then rejected the published APK
for `Found extra signing block 'Dependency metadata'`, the Google-encrypted
dependency list AGP embeds at signing time; 1.0.2 disables it with
`dependenciesInfo { includeInApk = false; includeInBundle = false }` so signed
release APKs now only carry the v2 signature and verity padding. Pinning the
build entry to the new fastlane commits then broke the reproducibility check
again: release APKs embed the full build commit in AGP's
`META-INF/version-control-info.textproto`, so the reference asset (built from
tag 1.0.2) no longer matched. 1.0.3 moves the listing edits into a tagged
release so the pin and the reference binary agree, and disables
`vcsInfo { include = false }` on release so binaries no longer embed the build
commit at all. The merge request to
`fdroid/fdroiddata` is ready to be opened from the pushed fork
branch `me.timschneeberger.shizustore`.

The September 2026 de-slop cleanup ran in eight phases (`CLEANUP.md`, commits
`3d10c45` to `2cf9b93`): 161 files changed, +820/-3097 lines. It removed the
dead AuroraDroid leftovers (screens, composables, repository facades, installer
seams, resource files), collapsed the four read-only list screens onto
`AppListScaffold`, trimmed never-used presentation-model fields, and dropped the
multi-mirror download vocabulary. 23 unit test classes remain, sharing
`RobolectricTestBase`/`ApiTestBase`; no em-dashes anywhere.

Per-architecture APK candidates (ABI) are supported end to end. `DownloadDto.abi`
flows into `AppDownloadEntity.abi` and is folded into `sigKey` (lowercased
suffix) so several architectures sharing one signing key stay distinct.
`AppCandidate.supportsAbi` accepts a null (universal) ABI or any token of
`AppCandidate.abiTokens` in `AppCandidate.deviceAbis` (`Build.SUPPORTED_ABIS`,
empty off-device so JVM tests never filter). `abiTokens` splits the raw F-Droid
index `nativecode` value on commas/whitespace and matches case-insensitively,
because index-only rows can carry a whole ABI list in one string; treating it as
a single token marked universal APKs incompatible. The ABI is not shown in the
source subtitle. `DetailedApp.primaryCandidate` and
`UpdateStateRepository.applyState` prefer a device-compatible candidate and
otherwise fall back to the previous first-by-primary behavior.
`CatalogUiMapper.toSources` groups the server's per-ABI rows by
`(packageName, versionCode, signer)` and keeps the device-compatible row, so
several architectures of one flavor render as a single source with that ABI
(`nativeCode`), not one source each. Tests: `AppDownloadEntityTest` abi suffix
cases, `AppCandidateTest` and `CatalogUiMapperTest`.

Per-flavor sources. APK flavors that share one APK
label (FOSS/Play/debug/release) arrive as separate `downloads[]` rows carrying
`packageName`; each is its own source row. `DownloadDto.packageName` maps into
`AppDownloadEntity.packageName` (unique index `(appSlug, packageName, sigKey)`),
and `AppCandidate.from` keeps the candidate's own package over the app fallback.
`Download.fromCatalog` and `CatalogUiMapper.candidateToResolvedApp` prefer the
candidate package, `UpdateStateRepository` resolves the installed app against the
canonical package then any candidate package (so installing a flavor marks the
entry installed and offers that flavor's update), and `SourceList` shows the
package as the source subtitle. `AppRepository.getByPackage` falls back to
`AppDao.getByDownloadPackage`, so a flavor package nav key still resolves to the
owning entry. Installed-app actions resolve the installed flavor package, not the
catalog row: `AppRepository.installedPackageFor` picks the canonical or candidate
package that has an installed row, `AppDetailsUiState.Loaded.actionablePackage`
carries it, and detail Open/Uninstall/App info/home shortcut plus the Updates row
target it. Tests: `UpdateStateRepositoryTest` flavor cases,
`DownloadFromCatalogTest`, `AppCandidateTest` and the `CatalogDaoTest`
`installedPackageFor` case.

Shizuku setup card. The Apps tab shows a one-time `ShizukuPromptCard` while
Shizuku needs action: not installed (the action opens the Play listing for the
stock package), installed but stopped (the action opens the manager app resolved
by permission; a stopped service reports no permission even while the grant still
exists, so starting comes first) or running but not allowed (the action requests
the grant; on success it sets `PREFERENCE_INSTALLER_ID = Installer.SHIZUKU` and
hides the card). It is also hidden once Shizuku is installed, running and
allowed. A manager counts as installed when its package declares one of
`ShizukuInstaller.MANAGER_PERMISSIONS` (the stock permission first, then
Shizuku+'s `af.shizuku.plus.permission.API_V23`), resolved through
`packageManager.getPermissionInfo`, so renamed forks and apps hidden from package
enumeration are still found; `Sui.isSui()` remains the embedded case. The card is
gated by the persisted `PREFERENCE_SHIZUKU_CARD_DISMISSED` flag, which defaults
to false, so a data reset re-shows it. There is deliberately no prompt to switch
the installer mode: granting through the card already selects Shizuku. In
addition, on the first launch after a data reset (`PREFERENCE_INSTALLER_ID`
unset) `ShizuApp` selects `Installer.SHIZUKU` when Shizuku is available and
already allowed, else persists `Installer.SESSION`; later launches keep the
stored choice. The state machine is `shizukuPrompt(available, running,
permitted)` with `running` from `ShizukuInstaller.isRunning()`
(`Shizuku.pingBinder`); tests: `ShizukuPromptTest`,
`ShizukuManagerPackagesTest`.

Custom installer source. Settings -> App installer carries a "Use custom
installer source" switch below the installer picker, off by default
(`PREFERENCE_INSTALLER_CUSTOM_SOURCE`), with a package-name field
(`PREFERENCE_INSTALLER_CUSTOM_SOURCE_PACKAGE`, shown as `com.android.vending`
until edited). The switch is enabled only while the Shizuku installer is
selected; the stored flag and package are kept when another installer is
active. Saving the field validates the name and confirms with the
`settings_installer_custom_source_saved` toast; an invalid name shows an
inline error instead. The screen uses `imePadding()` plus an IME-triggered
`bringIntoView` so the field stays above the keyboard. `ShizukuInstaller.resolveInstallerPackage`
reads both preferences per install and passes the result as the hidden
`PackageInstaller` wrapper's installer package name (`PackageInstallerHidden`
second argument), so a change takes effect without a restart. It falls back to
the app's own package when the flag is off or the field is blank.
`effectiveInstallerSourcePackage` is the pure decision function; test:
`ShizukuInstallerSourceTest`. Settings screens pass `titleColor = primary` to
`SectionHeader` so their category titles stand out; elsewhere it keeps the
default on-surface title color. The settings Permissions list also uses
`SectionHeader` (Required/Optional) instead of the old `TextDivider`.

Self-update. The published store package is hidden from browse/search and the home
carousels on all variants (base id, so debug/nightly hide the release entry too)
while staying in Installed and Updates; a self-replace is settled by
`SelfUpdateReceiver`. Tests: `AppListQueryBuilderTest` exclusion cases and
`SelfUpdateGuardTest`.

Changelog. The server collects a detail-only `changelog` (GitHub/GitLab release
markdown body, else the F-Droid/Izzy index long description) and sends it on
`GET /v1/apps/{slug}`. The client treats it like the README: never persisted,
kept in a bounded in-memory LRU (`DetailedAppRepository.changelog`), injected
onto `AppDetails` by `AppDetailsViewModel`. A `ChangelogScreen` mirrors the
"More about this app" screen and renders through `MarkdownDescription`; the code
forges stay on the markdown path while F-Droid/Izzy HTML uses
`AnnotatedString.fromHtml` (`assumeHtml`, selected by the new `AppDetails.sourceKind`).
The details screen shows a "Changelog" row only when the text is non-blank.
Tests: `DetailedAppRepositoryTest` (LRU hit) and
`MarkdownDescriptionTest.changelogRendersAsHtml`.

Relative ages in list rows. Browse/search rows already showed stars when sorted
by stars and installs when sorted by popularity; a row sorted by
`RECENTLY_ADDED`/`RECENTLY_UPDATED` now shows the matching timestamp ("3 days
ago") in the same meta line. `AppListItem`'s `AppAge.LISTED`/`RELEASED` picks
`listUpdatedAt`/`versionUpdatedAt`; the ISO-8601 wire strings are parsed once in
`CatalogUiMapper.toResolvedApp` into `ResolvedApp.listUpdatedAtMillis`/
`versionUpdatedAtMillis` (`CommonUtil.parseIsoUtcMillis`), and `relativeAge`
formats them from the `time_*_ago` plurals. Because `java.time` needs API 26,
core library desugaring is enabled (`desugar_jdk_libs` 2.1.5). Room columns and
the lexicographic ordering stay untouched. Tests: `CommonUtilTest` (parser
formats, bucket boundaries) and `CatalogUiMapperTest` (mapping).

Open in browser. The changelog and "More about this app" screens carry a top
bar action that opens an external page in a Custom Tab through
`SourceLauncher`. "More about" uses the app's forge/listing page
(`AppDetails.browserUrl`, `sourceUrl` then `url`); the changelog screen prefers
the release page the notes came from (`AppDetails.changelogBrowserUrl`, i.e.
`changelogUrl` then `browserUrl`). The server sends that URL as detail-only
`changelog_url` (GitHub `html_url`, GitLab `_links.self`; null for F-Droid/Izzy
index notes) and the client keeps it in a bounded LRU
(`DetailedAppRepository.changelogUrl`) that `AppDetailsViewModel` injects. The
action is hidden while loading and when no URL exists.

Detail row icons. `SectionHeader` gained an optional `icon` (24dp, primary tint,
16dp gap) and the details screen uses it on its rows: More about
(`ic_info_outlined`), Changelog (`ic_updates`), Permissions (`ic_shield`), the
Sources expander (`ic_deployed_code_update`) and the `LinkList` rows (source
`ic_code`, website/F-Droid `ic_language`, store `ic_storefront`; `ic_language` is
ported from `../auroradroid/`). The author and category carousels keep icon-less
headers. Link rows now carry their icon, and the legacy
`AppDetails.issueTracker`/`translation`/`donate` fields (always null or empty
from the mapper) were removed together with their strings and `LinkList` rows.

Screenshots. The server collects a detail-only `screenshots` list from the
F-Droid and Izzy `index-v2.json` (matched by every package name an app
publishes, primary plus variants) and sends it on `GET /v1/apps/{slug}`. The
client keeps it in the same kind of bounded in-memory LRU
(`DetailedAppRepository.screenshots`) and `AppDetailsViewModel` injects it onto
`AppDetails`. The details screen shows a `ScreenshotGallery` section (only when
non-empty) with a horizontal preview row and a fullscreen `HorizontalPager`
viewer; each image shows a loading spinner until it decodes. Tests:
`DetailedAppRepositoryTest` (LRU hit).

Store listing assets. September 2026: four app screenshots live in
`fastlane/metadata/android/en-US/images/phoneScreenshots/` (1 browse home,
2 search, 3 and 4 app details) and the README "Screenshots" section embeds
them. The 1080x2325 phone captures exceed the 2:1 edge ratio the F-Droid/Izzy
linters allow, so each was padded to 1164x2325 by replicating its outer pixel
columns; keep that constraint when replacing them.

Install reporting toggle. Settings -> Network carries a "Report installs" switch
below the proxy row, on by default (`PREFERENCE_INSTALL_REPORTING`, backed by the
same `ProxyViewModel` that serves the screen). `InstallReporter.reportInstalled`
reads the preference before the once-guard and the `POST /v1/apps/{slug}/installs`,
so both the installer callbacks and `InstallReconciler` stay gated at one
chokepoint; a failed preference read defaults to on. The Network summary string
now mentions install reporting. Test:
`InstallReporterTest.disabledReportingSendsNothing`.

Closed-source listing. A second Settings -> Network switch ("Show closed-source
apps", off by default, `PREFERENCE_SHOW_CLOSED_SOURCE`) makes sync ask for
`listing=main,closed_source`; otherwise every request sends `listing=main`. The
set flows into the bootstrap page, `/v1/changes` and `/v1/categories` via
`CatalogSyncer.listingParam`; toggling it calls
`CatalogSyncer.onShowClosedSourceChanged`, which deletes cached
`Listing.CLOSED_SOURCE` rows on opt-out, recomputes update state and clears the
cursor so the next sync re-bootstraps under the new listing set (closed rows
removed while the opt-in was off would never arrive as deltas). Tests:
`CatalogSyncerTest.bootstrapAsksForMainListingByDefault`,
`bootstrapAsksForClosedSourceWhenEnabled`,
`disablingClosedSourceDropsRowsAndResetsCursor`.

Remote catalog purge. `GET /v1/changes` carries `catalogPurgeRequestedAt`, the
server's `config_flags` high-water mark for remote purges. When the parsed value
is strictly newer than `PREFERENCE_LAST_CATALOG_PURGE_AT` (DataStore, so it
survives the wipe), `CatalogSyncer` records the marker first, drops `app`
(cascades `app_download`), `category` and `sync_state`, skips that delta payload
and bootstraps in the same run. The category ETag is not reused after a purge or
a 304 could leave the tree empty. Favourites, the blacklist and ignored updates
are user data and are never touched. Tests:
`CatalogSyncerTest.remotePurgeWipesCatalogAndBootstrapsPreservingUserData`,
`remotePurgeAppliesOnceThenSyncsIncrementally`, `olderPurgeTimestampIsIgnored`,
`malformedPurgeTimestampIsIgnored`.

Closed-source detail treatment. `AppDetails.listing` now reaches the UI
(`CatalogUiMapper.toAppDetails`). `LinkList` keeps a forge `sourceUrl` out of
the Source code row for `Listing.CLOSED_SOURCE` (`isSourceCodeLink`) and shows
it as a Website row instead, since those repos host APK releases rather than
sources. The details screen also shows a `ClosedSourceNotice` card
(`ic_shield_person`, tertiary container) stating the code is not public and
cannot easily be community-verified. Tests:
`LinkListTest.forgeLinksAreNotSourceCodeForClosedSourceApps`,
`CatalogUiMapperTest.appDetailsCarriesListing`.

Donations. The overflow sheet (`compose/ui/main/MoreSheet.kt`) carries a
"Support ShizuStore" entry directly above About, and the Settings screen has a
footer `DonationCard`; both open the shared `DonationDialog`
(`compose/composable/DonationDialog.kt`), a titled thank-you with PayPal
(`paypal.me/timschneeberger`) and Ko-fi
(`ko-fi.com/thepbone`) rows. The PayPal drawable is untinted and gets a white
outline built from eight offset copies of the painter (its dark brand colours
would vanish on the dark dialog); Ko-fi renders `ic_kofi_symbol` as-is because
that image already carries its own outline. Links open in a Custom Tab through
`SourceLauncher`. No automatic prompt, only on demand.

Search home. `AppListViewModel.atSearchHome` is an explicit place, not "no
filters set": clearing a category, sort or price keeps the list, and only the
back button (actionbar or system, via `SearchBackHandler`) returns to the tag
cloud and recent searches. `setQuery` leaves search home only for a non-blank
query; a blank one must not flip the flag because the text field's debounced
observer fires right after the back button clears it, which used to bounce the
screen from the home to an empty list.

App list scroll restore. Returning from a detail screen sometimes landed the list
near the top. Three layers were involved. Every successful sync upserts
`sync_state`, and `observePopularityFlag()` re-emitted even when the flag value
was unchanged, so `flatMapLatest` in `AppListViewModel.apps` rebuilt the `Pager`
and `cachedIn` dropped the loaded pages; both the flag flow and the
`(args, flag)` combine are now `distinctUntilChanged()`, so equal inputs never
recreate the Pager. A first-time detail open writes the app row, which
invalidates the Room paging source and anchors a refresh mid-list. Since
`enablePlaceholders` is false, Paging maps the UI anchor to a window-relative
position, so a refresh after the window had been rebased once asked Room for a
key near zero and reset the list to the first page. `WindowAwarePagingSource`
wraps the list's `@RawQuery` source and shifts the delegate refresh key by the
loaded window start (each Room page carries its absolute offset as `prevKey`),
which keeps the refresh on the row the user was on. As a second net,
`AppListScreen` records the first visible row (slug plus pixel offset) in the
ViewModel on dispose and, on return, finds it by key once paged data is present
(`ScrollAnchor`).

Carousel refresh. Pull-to-refresh updated Room but the home carousels kept
showing the old ranking: keyed `LazyRow`/`LazyHorizontalGrid` items anchor the
previously first visible tile by key, so when a sync moved an app up the row the
anchored item was scrolled back into view and the newly ranked apps stayed off
the left edge. A badge change on an existing tile still rendered, which made it
look like only reorders were ignored. `AppCarousel` now keeps a
`rememberLazyListState`/`rememberLazyGridState` per strip and calls
`scrollToItem(0)` from a `LaunchedEffect` keyed on the leading tile's slug, so a
changed ranking is shown from the start. The curated rows also reshuffle on a
user refresh: `AppsViewModel` holds `recommendedSeed`/`randomSeed` as
`MutableStateFlow`s that `retrySync()` re-randomizes before
`syncHelper.refresh()`, while background syncs keep the current order.

Drawables. September 2026: the Material icons under `res/drawable/` were
replaced from a newer Material Symbols download (the `new_icons/` folder at the
workspace root). Some picks are deliberately not 1:1: `ic_installed` now draws
the install-done glyph (used by the install-complete and confirm notifications;
the in-progress install notification shows the framework download icon like
ongoing downloads), `ic_group_network`
uses `network_manage` and `ic_cancel` the circle-X, which finally separates it
from `ic_clear`. The old duplicates are gone: `ic_block_outlined`,
`ic_favorite_border_outlined` and `ic_install_done` were deleted and their call
sites repointed to `ic_block`, `ic_favorite_unchecked` and `ic_installed`. The
download's `chevron_backward_24px` has no call site and was not added. The
drawables carry no SPDX or provenance headers (removed on request; the legacy
Aurora Store GPL blocks in `ic_arrow_right`/`ic_shield` are untouched). The
appcompat-only `?attr/colorControlNormal` tint was normalised to the framework
`?android:attr/colorControlNormal` on files that already tinted, and left off
everywhere else.

Compose icons. September 2026: every `androidx.compose.material.icons.Icons`
usage outside `CategoryIcons.kt` was replaced with drawables, using a second
`new_icons/` batch (`ic_attach_money`, `ic_category`, `ic_check_circle`,
`ic_cloud_off`, `ic_editor_choice`, `ic_money_off`, `ic_paid`, `ic_redeem`,
`ic_refresh`, `ic_schedule`, `ic_sd_card`, `ic_sort_by_alpha`, `ic_star`,
`ic_storefront`, `ic_warning`); favourite, check, update, info, clear,
open_in_new and download reuse the drawables above. `FilterOption` now carries
an optional `@DrawableRes iconRes` next to the `ImageVector icon` that
`CategoryIcons.kt` still supplies for the category rows. Two new list
categories got mapping entries: `android-auto` -> `ic_car` and
`shizuku-implementations` -> `ic_shizuku_icon`. `ic_car` arrived with the
appcompat-only `?attr/colorControlNormal` tint, which fails resource linking in
this theme, so the attribute was dropped; Compose `Icon` tints it like every
other category drawable.

Launcher icon. Original SVG masters live in `artwork/` (five colour variants,
v1 classic, v2 tilted, v3 peek, v4 minimal, v5 hide, plus a monochrome layer
test), drawn on the Android adaptive icon grid (108 x 108 units, safe circle
r=33). No design is chosen yet and none of it is wired into `res/`: the
launcher still ships the AuroraDroid PNGs.

Localization. September 2026: Simplified (`values-b+zh+Hans`), Traditional
(`values-b+zh+Hant`) and German (`values-de-rDE`) translations were added with AI
assistance and reviewed against the base strings. Key and plural parity holds
in both directions, format specifiers and `xliff:g` tags match, and the 20
`translatable="false"` entries live only in `values/strings.xml`. New strings
must be added to all four files. `androidResources.generateLocaleConfig` is
enabled and `res/resources.properties` pins `unqualifiedResLocale=en`, so AGP
derives `LocaleConfig` from the `values-*` folders at build time and wires it
into the manifest; adding a language folder needs no extra bookkeeping. There
is no in-app picker, so Android 12 and lower still follow the system language,
and lint's `MissingTranslation` check stays disabled so a partial translation
never fails the build.

Home category sections. `AppsViewModel` appends one `AppGroupKind.CATEGORY` strip
per qualifying top-level category after the curated rows. The pure
`CategorySections` builder keeps roots whose subtree (all descendants, via
`CategoryTag.flatten()`) has at least 4 apps, orders sections by member count then
name, sorts each section by install count when the popularity flag is on and
`downloadTotal` (nulls last) otherwise, and takes 20. `AppGroup` carries the
category display name in a new `title` (the `category` field stays the slug for
navigation). `AppRepository.observeAllApps()` supplies the self-filtered catalog;
`ResolvedApp` gained `hasAds`/`downloadTotal`/`categorySlug` for this and the
badges below. Tests: `CategorySectionsTest`.

Ads and detail counts. `ResolvedApp.hasAds` and `AppDetails.hasAds` come from the
sync DTOs and drive a new "Ads" badge on list rows and the
`BillingNotice`/`details_ads` sentence on the details card (which now covers
paid, IAP and ads). `DetailsStats` renders up to four metric cells (installs,
downloads, stars, size) between the install section and the compatibility notice,
omitting zero or unknown values. The chip row keeps only category, license and
Android version, and the updated date moves into the header version line
(`details_updated`). `AppDetails` gained `installCount`/`downloadTotal`/
`versionUpdatedAtMillis`. Strings were added to all four locale files; tests:
`CatalogUiMapperTest`.

Install reporting with version and type. `POST /v1/apps/{slug}/installs` now
sends `{"versionCode", "installType"}` (`InstallReportDto`, `InstallType` enum
with wire values `fresh`/`update`/`unknown`). `OkHttpShizuApi.post(path, body)`
sends a JSON body when present and stays backwards compatible with the empty
body. `InstallReporter` resolves the type from `PackageManager`
(`firstInstallTime != lastUpdateTime` means update, zero timestamps mean unknown)
through the pure `resolveInstallType`, so failures degrade to unknown; the
`(package, versionCode)` once-guard is unchanged, and the server defaults a
missing body to `0`/`unknown`. Tests: `InstallReporterTest`.

APK analysis signals. The server now analyzes each APK's declared permissions and
DEX against the Exodus tracker code signatures and exposes `dhizukuDeclared` and
`trackers` on summaries, details and downloads. `AppEntity` gained
`dhizukuDeclared`/`trackers` (Room v2, additive `MIGRATION_1_2`), the DTOs and
`CatalogMappers` carry them, and `ResolvedApp`/`AppDetails` surface them. List
rows show a "Trackers" badge when the primary APK matched any signature, the
details chip row adds a "Dhizuku" chip when the app declares the Dhizuku
permission, and `TrackersNotice` lists the matched tracker names with a note that
detection is code-signature based (not a complete list). Tests:
`CatalogUiMapperTest`.

## Compose performance pass (September 2026)

A source-level recomposition audit (no compiler stability reports are enabled;
strong skipping is on via Kotlin 2.4) drove a scoped fix pass. Rules that now hold:

- `AppDetailsUiState.Loaded` no longer carries `download`. `AppDetailsViewModel.download`
  is a separate `StateFlow` collected inside `InstallSection`, so download progress
  ticks no longer rebuild the details state and retrigger the page `AnimatedContent`.
  `rememberCanOpen` keys on package + installed state only, not download status.
- Download rows read an `@Immutable DownloadRowState` snapshot
  (`Download.toRowState()`), so `DownloadListItem` and `AppUpdateItem` can skip
  while the mutable Room entity (`var status/progress/...`) stays out of composable
  parameters. `downloadRow` results are `remember`ed per snapshot.
- `AppDetails`, `AppSource`, `CategoryTag`, `AppGroup` and `DetailedApp` are
  `@Immutable` (their lists are replaced, never mutated). `Download` and
  `AppDetailsUiState.Loaded` are deliberately not annotated.
- Paging phase reads (`loadState`/`itemCount`) live in child composables
  (`AppRows`, `DownloadsContent`, `UpdatesBody`, `InstalledContent`,
  `FavouritesContent`), never in a screen body. `WindowInsets.isImeVisible` is read
  inside `SearchBackHandler` so keyboard animations do not invalidate the screen.
- Per-frame animation values are applied in `graphicsLayer {}` lambdas
  (`AnimatedAppIcon`, `SourceList`), not read in composition.
- `ResolvedApp` parses its fingerprint sets once; `CommonUtil.formatDate` shares a
  ThreadLocal `DateFormat`; filter options, `flatten()` results, detail tags, link
  rows and markdown typography/components are `remember`ed.
- Markdown passes a remembered `GFMFlavourDescriptor`/`MarkdownParser` so an
  unchanged document is not reparsed; README images remember their request.
- Screenshot strip requests carry an explicit size (avoiding the Coil constraints
  SubcomposeLayout path), the viewer prefetches one page, and `ScreenshotImageKeys`
  is a bounded LRU.

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
when the stored ETag changes and advances the cursor to `meta().generatedAt`.
`/v1/changes` and `/v1/apps` carry summaries only, so `DetailedAppRepository`
fetches `/v1/apps/{slug}` for candidates, the full description, the changelog
and the screenshots. The README, changelog and screenshots are kept only in
bounded in-memory LRUs, never persisted to Room.

**Install.** `DownloadHelper` stages a queue row from a candidate. `DownloadWorker`
fetches the upstream `apkUrl`, verifies its hash, and extracts the named member
first when the release ships the APK inside an archive. `InstallWorker` hands the
file to the selected installer. APKs are removed once the package manager confirms
the install, and `InstallReconciler` sweeps stranded rows and files.

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

- The Room database is `ShizuStoreDatabase` (schema `ShizuStoreDatabase`), reset
  to version 1 with no migrations: fresh installs create the full catalog schema
  in one step and there is no upgrade path from the pre-release builds. Old
  `AuroraDatabase` migrations and schemas are gone; export schemas to
  `app/schemas/` as usual.
- When `candidate.archiveEntry` is set the server `sha256`/`size` describe the
  archive, not the APK. Verify the archive hash first, then extract the entry; both
  the `.apk` and the staged `.archive` are cleaned up on failure or cancel.
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
  here. `ktlintCheck` currently fails on pre-existing violations in
  `AppListItem.kt` (line 65), `DonationDialog.kt` (141-143) and `CommonUtil.kt`
  (170, 175); the scroll-restore change did not introduce them.
- `auroradroid/` is reference only; never edit or bulk-copy it over this fork.

## Known deviations

Tracked in `TODO.md`. In short: the `ResolvedApp`/`AppDetails`/`AppSource`
presentation shim is retained and filled by `CatalogUiMapper`, and the database
stays at version 1 with the legacy write-only columns (the unused `Download`
columns and the `AppEntity`/`CategoryEntity`/`SyncStateEntity` leftovers listed
in `TODO.md`) still in place to avoid a Room schema bump.

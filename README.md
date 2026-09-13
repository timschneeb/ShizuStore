<img src="https://i.imgur.com/kSApIjL.png" height="128" alt="Aurora Logo"><br/><img src="https://www.gnu.org/graphics/gplv3-88x31.png" alt="GPL v3 Logo">

# Shizu Store

> **Fork notice.** Shizu Store is a fork of
> [Aurora Droid](https://gitlab.com/AuroraOSS/auroradroid) (GPL-3.0-or-later,
> upstream HEAD `32385c8`). The F-Droid multi-repo index sync and repository
> management have been replaced by the `ShizuAppStoreServer` `/v1/*` REST API.
> The browse, search, details, download and installer flows, including the
> session, root and Shizuku installers, are inherited from Aurora Droid. See
> `PLAN.md` for the migration plan and `HANDOFF.md` for current state.

*Shizu Store* is a sideloaded store for [Shizuku](https://shizuku.rikka.app/)
apps. It indexes the awesome-shizuku list through the Shizu backend and installs
APKs from their upstream sources.

Aurora Droid is another FOSS client for F-Droid. It works with the same
repositories as the official client and presents them through a Material 3
interface.

Version 2.x is a full rewrite: new interface, new database, and new sync,
download and install paths. Nothing carries over from 1.x.

Aurora Droid is not affiliated with or endorsed by F-Droid. Shizu Store is not
affiliated with Aurora OSS.

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/me.timschneeberger.shizustore/)

Nightly builds are at [AuroraOSS](https://auroraoss.com/files/ShizuStore/Nighty) and in the
[Telegram group](https://t.me/ShizuStore).

## Screenshots

<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/ss001.png" height="400"><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/ss002.png" height="400">
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/ss003.png" height="400"><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/ss004.png" height="400">
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/ss005.png" height="400"><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/ss006.png" height="400">

## Features

**One catalogue across every repository.** Apps from all enabled repositories appear in a single
list, organised by category, with recently added and recently updated sections. Search covers all of
them at once.

**Repository management.** Add any F-Droid repository by pasting its link, then enable, disable,
reorder or remove it at any time. Where two repositories carry the same app, the one placed higher
in the list takes precedence. F-Droid is enabled by default; IzzyOnDroid and microG are included but
switched off.

**Four install methods.** The default session installer, the system installer, root, and Shizuku.
Root and Shizuku install without a confirmation dialog for each app.

**Scheduled update checks.** Repositories are checked on an interval of your choosing, from every
6 hours to weekly, or not at all. On devices that support silent installs, updates can be downloaded
and applied automatically. All background activity can be restricted to Wi-Fi.

**Compatible updates only.** Versions built for a different CPU architecture, or requiring a newer
Android release than the device runs, are not offered as updates.

**A full download manager.** Progress, cancellation, retries and queue management, with automatic
fallback to a repository's mirrors when its primary server is unavailable.

**Grouped notifications.** A batch of updates produces a single summary rather than one notification
per app.

**Favourites, blacklist and ignored updates.** Mark the apps you follow, hide packages you never
want to see, and skip a specific version of an individual app.

**No tracking.** No analytics, no accounts, no third-party services. Every APK is verified against
the checksum published by its repository before installation, and repositories are pinned to their
signing key, so a substituted server cannot serve different content.

**Free software.** GPLv3, developed in the open, with no proprietary components in the build.

## Requirements

| Requirement | Version |
|---|---|
| Android | 7.0 (API 24) and above |
| Target/compile SDK | 37 |
| JDK | 21 (the Gradle toolchain resolves it automatically) |
| Gradle | via `./gradlew`, AGP 9.x, Kotlin 2.4.x |

## Building

```bash
git clone https://gitlab.com/AuroraOSS/auroradroid.git
cd auroradroid
./gradlew assembleDebug
```

The APK lands in `app/build/outputs/apk/` as `ShizuStore-<version><-type><-unsigned>.apk`.

### Build types

| Type | Application ID | Notes |
|---|---|---|
| `debug` | `me.timschneeberger.shizustore.debug` | Signed with the tracked AOSP `testkey.jks`, so debug builds are interchangeable across machines |
| `release` | `me.timschneeberger.shizustore` | Minified and resource-shrunk |
| `nightly` | `me.timschneeberger.shizustore.nightly` | Release settings, version name suffixed with the short commit hash |

Release and nightly builds are signed only if a `signing.properties` exists in the project root;
without it they are produced unsigned and the file name says so. The file is gitignored and looks
like this:

```properties
STORE_FILE=/path/to/keystore.jks
STORE_PASSWORD=...
KEY_ALIAS=...
KEY_PASSWORD=...
```

### Checks

```bash
./gradlew ktlintCheck    # style, `ktlintFormat` to fix
./gradlew lintDebug      # Android lint, config in app/lint.xml
```

Room schemas are exported to `app/schemas/`; commit the new JSON whenever the database version
changes.

## Project layout

Everything lives in the single `:app` module, under `me.timschneeberger.shizustore`.

| Package | What is in it |
|---|---|
| `compose/ui/` | One package per screen (apps, categories, updates, details, downloads, repositories, search, settings, about, installed, favourites, blacklist, ignored) |
| `compose/composable/` | Shared widgets: list items, top bars, placeholders, shimmer, permission lists |
| `compose/navigation/` | `Screen` keys and the Navigation 3 `NavDisplay` |
| `compose/theme/` | Material 3 expressive theme, dynamic colour, light/dark |
| `viewmodel/` | One ViewModel per screen, Paging 3 where lists can grow |
| `data/sync/` | Repository index: download, fingerprint verification, streaming parse, v2 diff merge, persistence |
| `data/network/` | OkHttp client, conditional index downloads, byte-range aware downloader, proxy support |
| `data/download/` | Mirror resolution and APK hash verification |
| `data/installer/` | Session, native, root and Shizuku installers behind one `IInstaller` |
| `data/work/` | `SyncWorker`, `DownloadWorker`, `InstallWorker`, `UpdateWorker` |
| `data/room/` | Database, DAOs and entities (repo, app, version, installed, download, favourite, blacklist, ignored, anti-feature) |
| `data/repository/` | Query surface the ViewModels talk to |
| `data/helper/` | Sync, download and install orchestration, update batching |
| `data/receiver/` | Install status, package add/remove, download cancel and retry |
| `util/`, `extensions/` | Preferences (DataStore), notifications, paths, shortcuts, small Kotlin/Android extensions |

### How a sync works

1. `SyncWorker` runs per repository, either on demand or from a scheduled `UpdateWorker` sweep.
2. `RepoSyncer` tries `EntrySyncable` first. It fetches `entry.jar` conditionally, so an unchanged
   repository costs one 304 response. Where the entry offers a diff against the timestamp already
   held, only the diff is fetched and merged as an RFC 7386 JSON merge patch.
3. A 404 on `entry.jar` falls back to `V1Syncable` and `index-v1.jar`, which is converted to the v2
   shape in memory.
4. The JAR's signing certificate is hashed and matched against the repository fingerprint. A
   repository added without a fingerprint pins the one it presents on first sync; a mismatch after
   that fails the sync.
5. `IndexPersister` streams the index into Room in a single transaction, so a failure part-way
   leaves the previous catalogue intact.

### How an install works

`DownloadWorker` resolves the APK URL (falling back through the repository's mirrors), downloads it
to `filesDir/apk/`, and verifies the SHA-256 from the index. `InstallWorker` then hands the file to
the selected installer. Downloaded APKs are removed once the package manager confirms the install.

## How to

**Add a repository.** More → Repositories → the add button. Paste either a plain URL or a full
F-Droid link carrying the fingerprint, for example
`https://example.org/fdroid/repo?fingerprint=ABCD...`. `fdroidrepo://` and `fdroidrepos://` links
are accepted too. Without a fingerprint the first sync pins whatever the repository presents, which
is trust-on-first-use, so prefer the fingerprint form when you have it.

Aurora Droid ships with F-Droid enabled, and IzzyOnDroid and microG present but disabled.

**Reorder repositories.** Drag them in the repositories list. The one nearer the top wins when the
same package is published by more than one repo.

**Install without confirming every app.** Settings → App installer, then pick Root or Shizuku.
Session and System both go through Android's own confirmation dialog and cannot be silent. Shizuku
takes a little setup, covered [below](#setting-up-the-shizuku-installer).

**Turn on automatic updates.** Settings → Updates → Install updates automatically. It stays paused
unless background checks are on *and* the selected installer can install without confirmation, and
the screen tells you which of the two is missing.

**Change how often updates are checked.** Settings → Updates → Update checks: never, every 6 hours,
every 12 hours, daily or weekly. "Check on Wi-Fi only" makes background checks and automatic
downloads wait for an unmetered network.

**Use a proxy.** Settings → Network → Proxy, as `protocol://host:port` with an optional
`user:password@`. `http`, `https`, `socks` and `socks5` are supported, and it takes effect on the
next connection.

**Stop an app from nagging.** Open the app and ignore the update to skip that one version, or use
the blacklist to hide the package entirely. Both lists are under More.

## Setting up the Shizuku installer

[Shizuku](https://shizuku.rikka.app/) lends Aurora Droid the system privileges it needs to install
apps without a confirmation dialog, on a device with no root. It is what makes fully automatic
updates possible on a stock phone.

You need Android 8.0 or later.

**1. Install and start Shizuku.**

Get it from [F-Droid](https://f-droid.org/packages/moe.shizuku.privileged.api/) or the
[project site](https://shizuku.rikka.app/), open it, and start the service by whichever route your
device allows:

* **Wireless debugging** (Android 11 and later, no computer needed) - turn on Developer options,
  enable Wireless debugging, and follow the pairing steps Shizuku walks you through.
* **ADB from a computer** - enable USB debugging, plug in, and run the command Shizuku shows you.
* **Root** - tap Start, grant superuser access, and you are done. If you are rooted, installing
  [Sui](https://github.com/RikkaApps/Sui) instead is smoother; Aurora Droid treats it exactly like
  Shizuku.

Started over ADB or wireless debugging, Shizuku stops at every reboot and has to be started again.
Started with root or Sui, it comes back on its own.

**2. Point Aurora Droid at it.**

Settings → App installer → Shizuku installer.

**3. Grant the permission.**

The first install asks Shizuku for access; allow it. If you miss the prompt, or something is off
later, Aurora Droid tells you which piece is missing rather than failing quietly.

**4. Optional: let updates install themselves.**

With Shizuku working, Settings → Updates → Install updates automatically stops being greyed out.
Pair it with an update check interval and new versions download and install in the background.

If installs start failing, the usual cause is Shizuku having stopped after a reboot. Open it and
start the service again.

## Translations

Aurora Droid is translated on Weblate:
[hosted.weblate.org/translate/aurora-droid](https://hosted.weblate.org/translate/aurora-droid).

[<img src="https://hosted.weblate.org/widget/aurora-droid/matrix-auto.svg"
      alt="Translation status per language">](https://hosted.weblate.org/translate/aurora-droid)

Adding a language or correcting a string needs no Git knowledge and no local setup; sign in and
start typing. Source strings live in `app/src/main/res/values/strings.xml` and are the only ones
edited by hand. Translated resources come back from Weblate, so please do not submit them as merge
requests.

## Credits

Aurora Droid reuses code from these projects, all GPL-3.0-or-later. Individual files carry the
attribution in their header.

* [Aurora Store](https://gitlab.com/AuroraOSS/AuroraStore) - installers (session, native, root,
  Shizuku), the download worker and download UI, notifications, navigation scaffolding, the
  settings screens and a number of shared composables.
* [Droid-ify](https://github.com/Droid-ify/client) - the repository sync path: index entry and v1
  handling, JAR fingerprint verification, and the OkHttp download layer.
* [Neo Store](https://github.com/NeoApplications/Neo-Store) - applying index-v2 diffs as RFC 7386
  JSON merge patches.
* [F-Droid](https://f-droid.org/) - the repositories, the index format and the specification
  everything above implements.

### Libraries

[Jetpack Compose](https://developer.android.com/compose) ·
[Material 3](https://m3.material.io/) ·
[Navigation 3](https://developer.android.com/guide/navigation) ·
[Room](https://developer.android.com/training/data-storage/room) ·
[Hilt](https://dagger.dev/hilt/) ·
[WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) ·
[Paging 3](https://developer.android.com/topic/libraries/architecture/paging/v3-overview) ·
[DataStore](https://developer.android.com/topic/libraries/architecture/datastore) ·
[kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) ·
[Coil](https://coil-kt.github.io/coil/) ·
[OkHttp](https://github.com/square/okhttp) ·
[libsu](https://github.com/topjohnwu/libsu) ·
[Shizuku](https://shizuku.rikka.app/) ·
[Refine](https://github.com/RikkaApps/Refine) ·
[HiddenApiBypass](https://github.com/LSPosed/AndroidHiddenApiBypass)

## Support development

<img src="https://img.shields.io/static/v1?label=Bitcoin&message=bc1qu7cy9fepjj309y4r2x3rymve7mw4ff39c8cpe0&color=Orange" alt="Bitcoin address">

<img src="https://img.shields.io/static/v1?label=Bitcoin%20Cash&message=qpqus3qdlz8guf476vwz0fjl8s34fseukcmrl6eknl&color=Success" alt="Bitcoin Cash address">

<img src="https://img.shields.io/static/v1?label=Ethereum&message=0x6977446933EC8b5964D921f7377950992337B1C6&color=Blue" alt="Ethereum address">

<img src="https://img.shields.io/static/v1?label=BHIM%20UPI&message=whyorean@dbs&color=BlueViolet" alt="BHIM UPI ID">

* PayPal - [Link](https://paypal.me/AuroraDev)
* LiberaPay - [Link](https://liberapay.com/whyorean/)

## Links

* Source - [GitLab](https://gitlab.com/AuroraOSS/auroradroid)
* Translations - [Weblate](https://hosted.weblate.org/translate/aurora-droid)
* Support group - [Telegram](https://t.me/ShizuStore)
* XDA forum - [Thread](https://xdaforums.com/android/apps-games/app-aurora-droid-fdroid-client-t3932663)

## License

GPL-3.0-or-later. See [LICENSE](LICENSE).

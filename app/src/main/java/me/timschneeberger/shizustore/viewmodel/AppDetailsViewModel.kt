/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.timschneeberger.shizustore.data.helper.DownloadHelper
import me.timschneeberger.shizustore.data.helper.InstallDispatcher
import me.timschneeberger.shizustore.data.model.AppDetails
import me.timschneeberger.shizustore.data.model.AppSource
import me.timschneeberger.shizustore.data.model.CertFingerprint
import me.timschneeberger.shizustore.data.model.InstallDispatch
import me.timschneeberger.shizustore.data.model.ResolvedApp
import me.timschneeberger.shizustore.data.model.preferredForThisDevice
import me.timschneeberger.shizustore.data.repository.AppRepository
import me.timschneeberger.shizustore.data.repository.BlacklistRepository
import me.timschneeberger.shizustore.data.repository.CatalogUiMapper
import me.timschneeberger.shizustore.data.repository.DetailedAppRepository
import me.timschneeberger.shizustore.data.repository.DetailedAppResult
import me.timschneeberger.shizustore.data.repository.FavouriteRepository
import me.timschneeberger.shizustore.data.repository.IgnoredUpdateRepository
import me.timschneeberger.shizustore.data.repository.InstalledRepository
import me.timschneeberger.shizustore.data.room.entity.Download
import me.timschneeberger.shizustore.data.room.entity.IgnoredUpdateEntity
import me.timschneeberger.shizustore.data.sync.CatalogSyncFailure

sealed interface AppDetailsUiState {
    data object Loading : AppDetailsUiState
    data object NotFound : AppDetailsUiState
    data class Error(val failure: CatalogSyncFailure) : AppDetailsUiState

    data class Loaded(
        val details: AppDetails,
        val sources: List<AppSource>,
        /**
         * The package the entry is actually installed under. An entry is keyed by
         * its canonical package, but a flavor installs under its own, so system
         * actions (open, uninstall, app info) must target this, not the key.
         */
        val installedPackage: String? = null
    ) : AppDetailsUiState {
        /** Resolved once: the getter ran on every read and the source list is fixed. */
        val resolved: ResolvedApp? = sources.preferredForThisDevice()?.app

        /** The package to act on for installed-app actions, falling back to the catalog key. */
        val actionablePackage: String get() = installedPackage ?: details.packageName
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AppDetailsViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val detailedAppRepository: DetailedAppRepository,
    private val downloadHelper: DownloadHelper,
    private val installDispatcher: InstallDispatcher,
    private val mapper: CatalogUiMapper,
    private val favouriteRepository: FavouriteRepository,
    private val blacklistRepository: BlacklistRepository,
    private val ignoredUpdateRepository: IgnoredUpdateRepository,
    private val installedRepository: InstalledRepository
) : ViewModel() {
    /** The navigation key: a real package name when known, otherwise the catalog slug. */
    private val identity = MutableStateFlow<String?>(null)
    private val slug = MutableStateFlow<String?>(null)

    /** Last detail-fetch failure, surfaced instead of silently showing stale data. */
    private val _detailError = MutableStateFlow<CatalogSyncFailure?>(null)
    val detailError: StateFlow<CatalogSyncFailure?> = _detailError.asStateFlow()

    /** User-triggered reload in progress; the pull wrapper keeps feedback visible briefly. */
    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    /** Detail request in progress; the screen stays blank until it settles. */
    private val detailFetching = MutableStateFlow(false)

    private val _refusals = Channel<InstallDispatch.Refused>(Channel.BUFFERED)
    val refusals: Flow<InstallDispatch.Refused> = _refusals.receiveAsFlow()

    val uiState: StateFlow<AppDetailsUiState> = slug
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { slug -> detailsFor(slug).onStart { emit(AppDetailsUiState.Loading) } }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            AppDetailsUiState.Loading
        )

    /**
     * Kept out of [uiState]: the details page must not be recreated for every
     * download progress tick, only consumers of the download row recompose.
     */
    val download: StateFlow<Download?> = slug
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { slug -> observeDownload(slug) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val isFavourite: StateFlow<Boolean> = identity
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { favouriteRepository.isFavourite(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    val isBlacklisted: StateFlow<Boolean> = identity
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { blacklistRepository.isBlacklisted(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    val ignoredUpdate: StateFlow<IgnoredUpdateEntity?> = identity
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { ignoredUpdateRepository.observeIgnore(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    // Other apps by the same stable developer key; empty until a profile is known.
    val moreFromAuthor: StateFlow<List<ResolvedApp>> = slug
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { currentSlug ->
            appRepository.observeDetail(currentSlug)
                .map { it?.app?.authorKey }
                .distinctUntilChanged()
                .flatMapLatest { authorKey ->
                    if (authorKey.isNullOrBlank()) {
                        flowOf(emptyList())
                    } else {
                        appRepository.observeByAuthor(authorKey, currentSlug)
                            .map { rows -> rows.map(mapper::toResolvedApp) }
                    }
                }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            emptyList()
        )

    val categorySlug: StateFlow<String?> = slug
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { currentSlug -> observeCategorySlug(currentSlug) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            null
        )

    // Other apps in the same category; empty until a profile is known.
    val moreFromCategory: StateFlow<List<ResolvedApp>> = slug
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { currentSlug ->
            observeCategorySlug(currentSlug)
                .flatMapLatest { category ->
                    if (category.isNullOrBlank()) {
                        flowOf(emptyList())
                    } else {
                        appRepository.observeByCategory(category, currentSlug)
                            .map { rows -> rows.map(mapper::toResolvedApp) }
                    }
                }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            emptyList()
        )

    private fun observeCategorySlug(currentSlug: String) = appRepository.observeDetail(currentSlug)
        .map { it?.app?.categorySlug }
        .distinctUntilChanged()

    fun load(packageName: String) {
        identity.value = packageName
        viewModelScope.launch {
            val resolvedSlug = appRepository.getByPackage(packageName)?.slug ?: packageName
            detailFetching.value = true
            slug.value = resolvedSlug
            if (detailedAppRepository.fullDescription(resolvedSlug).isNullOrBlank()) {
                fetchDetail(resolvedSlug)
            } else {
                // A fresh ViewModel (e.g. the full-description screen) already
                // holding the markdown must not refetch it over the network.
                detailFetching.value = false
            }
        }
    }

    fun retry() {
        val resolvedSlug = slug.value ?: return
        viewModelScope.launch {
            _refreshing.value = true
            try {
                fetchDetail(resolvedSlug)
            } finally {
                _refreshing.value = false
            }
        }
    }

    private suspend fun fetchDetail(resolvedSlug: String) {
        detailFetching.value = true
        try {
            when (val result = detailedAppRepository.fetchAndPersist(resolvedSlug)) {
                is DetailedAppResult.Success -> _detailError.value = null
                DetailedAppResult.NotFound -> _detailError.value = null
                is DetailedAppResult.Failed -> {
                    Log.w(TAG, "Detail fetch failed for $resolvedSlug: ${result.failure}")
                    _detailError.value = result.failure
                }
            }
        } finally {
            detailFetching.value = false
        }
    }

    fun toggleFavourite() {
        val identity = identity.value ?: return
        viewModelScope.launch { favouriteRepository.toggle(identity) }
    }

    fun toggleBlacklist() {
        val identity = identity.value ?: return
        viewModelScope.launch { blacklistRepository.toggle(identity) }
    }

    fun ignoreAllUpdates() {
        val identity = identity.value ?: return
        viewModelScope.launch { ignoredUpdateRepository.ignoreAll(identity) }
    }

    fun ignoreThisVersion() {
        val app = resolvedApp() ?: return
        viewModelScope.launch {
            ignoredUpdateRepository.ignoreVersion(app.packageName, app.versionCode)
        }
    }

    fun stopIgnoringUpdates() {
        val identity = identity.value ?: return
        viewModelScope.launch { ignoredUpdateRepository.stopIgnoring(identity) }
    }

    fun install() {
        val loaded = uiState.value as? AppDetailsUiState.Loaded ?: return
        val source = loaded.sources.firstOrNull { it.installedPackageMatch }
            ?: loaded.sources.firstOrNull { it.signerMatch }
            ?: loaded.sources.firstOrNull { it.app.candidateId != null }
            ?: return
        val app = source.app

        viewModelScope.launch {
            val stored = downloadHelper.getDownload(app.packageName)

            if (stored != null &&
                stored.versionCode == app.versionCode &&
                installDispatcher.canInstallFromDisk(stored)
            ) {
                dispatch(stored)
                return@launch
            }

            val entity = appRepository.get(app.slug) ?: return@launch
            val candidate = app.candidateId?.let { appRepository.candidate(it) } ?: return@launch
            downloadHelper.enqueueAndInstall(entity, candidate)
        }
    }

    fun cancel() {
        val identity = identity.value ?: return
        viewModelScope.launch { downloadHelper.cancel(identity) }
    }

    fun installFrom(source: ResolvedApp) {
        viewModelScope.launch {
            val entity = appRepository.get(source.slug) ?: return@launch
            val candidate = source.candidateId?.let { appRepository.candidate(it) } ?: return@launch
            downloadHelper.enqueueAndInstall(entity, candidate)
        }
    }

    private suspend fun dispatch(download: Download) {
        when (val result = installDispatcher.dispatch(download.packageName)) {
            InstallDispatch.Started -> Unit
            is InstallDispatch.Refused -> {
                Log.w(TAG, "Install of ${download.packageName} refused at ${result.status}")
                _refusals.send(result)
            }
        }
    }

    private fun resolvedApp(): ResolvedApp? = (uiState.value as? AppDetailsUiState.Loaded)?.resolved

    /** The download row is keyed by the flavor package, which is not always
     * the nav-key package, so match any package the entry ships. */
    private fun observeDownload(slug: String): Flow<Download?> = combine(
        appRepository.observeDetail(slug),
        downloadHelper.downloads
    ) { detailed, rows ->
        val identity = identity.value.orEmpty()
        val packages = buildSet {
            add(identity)
            detailed?.app?.packageName?.let { add(it) }
            detailed?.candidates?.forEach { candidate -> candidate.packageName?.let { add(it) } }
        }
        rows.firstOrNull { it.packageName in packages }
    }.distinctUntilChanged()

    private fun detailsFor(slug: String): Flow<AppDetailsUiState> {
        val catalog = combine(
            appRepository.observeDetail(slug),
            installedRepository.observeAll()
        ) { detailed, installedAll ->
            // The entry is keyed by its canonical package, but the user may have
            // installed a flavor, so match any package the entry ships.
            val installed = detailed?.let { detail ->
                val packages = buildSet {
                    detail.app.packageName?.let { add(it) }
                    detail.candidates.forEach { candidate ->
                        candidate.packageName?.let { add(it) }
                    }
                }
                installedAll.firstOrNull { it.packageName == detail.app.packageName }
                    ?: installedAll.firstOrNull { it.packageName in packages }
            }
            detailed to installed
        }

        return combine(
            catalog,
            _detailError,
            detailFetching
        ) { catalogPair, error, fetching ->
            val (detailed, installed) = catalogPair
            when {
                // Blank the screen until the request settles so the install row
                // does not pop in late; failures fall back to cached data.
                fetching -> AppDetailsUiState.Loading

                detailed != null -> {
                    val fingerprint = installed?.let { CertFingerprint.of(it.signer, it.signerMd5) }
                    AppDetailsUiState.Loaded(
                        details = mapper.toAppDetails(detailed).copy(
                            fullDescription = detailedAppRepository.fullDescription(slug),
                            changelog = detailedAppRepository.changelog(slug),
                            changelogUrl = detailedAppRepository.changelogUrl(slug),
                            screenshots = detailedAppRepository.screenshots(slug).orEmpty()
                        ),
                        sources = mapper.toSources(detailed, fingerprint, installed?.packageName),
                        installedPackage = installed?.packageName
                    )
                }

                // Nothing cached and the fetch failed: show the failure, not "not found".
                error != null -> AppDetailsUiState.Error(error)

                else -> AppDetailsUiState.NotFound
            }
        }
    }

    private companion object {
        const val TAG = "AppDetailsViewModel"
    }
}

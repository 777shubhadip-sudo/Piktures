package com.example.ui.viewmodel

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import com.example.data.local.FavoriteEntity
import com.example.data.local.SettingsRepository
import com.example.data.local.VaultDao
import com.example.data.repository.MediaStoreRepository
import com.example.domain.model.AlbumFolder
import com.example.domain.model.AppSettings
import com.example.domain.model.GalleryMedia
import com.example.domain.model.GalleryTab
import com.example.domain.model.MediaDetails
import com.example.domain.model.MediaItemRef
import com.example.domain.model.MediaPermissionState
import com.example.domain.model.MediaType
import com.example.domain.model.RelockTimeout
import com.example.domain.model.ThemeMode
import com.example.domain.model.UiColorPalette
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import javax.inject.Inject

enum class SearchFilterChip(val label: String) {
    ALL("All"),
    PHOTOS("Photos"),
    VIDEOS("Videos"),
    FAVORITES("Favorites")
}

sealed interface PendingMediaRequest {
    val pendingIntent: PendingIntent

    data class Trash(override val pendingIntent: PendingIntent) : PendingMediaRequest
    data class RestoreFromTrash(override val pendingIntent: PendingIntent) : PendingMediaRequest
    data class PermanentDelete(override val pendingIntent: PendingIntent) : PendingMediaRequest
    data class RenameWrite(
        override val pendingIntent: PendingIntent,
        val uri: Uri,
        val newName: String
    ) : PendingMediaRequest
}

@HiltViewModel
class GalleryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaStoreRepository: MediaStoreRepository,
    private val vaultDao: VaultDao,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _permissionState = MutableStateFlow(mediaStoreRepository.checkPermissionState())
    val permissionState: StateFlow<MediaPermissionState> = _permissionState.asStateFlow()

    private val _currentTab = MutableStateFlow(GalleryTab.PHOTOS)
    val currentTab: StateFlow<GalleryTab> = _currentTab.asStateFlow()

    private val _rawMedia = MutableStateFlow<List<GalleryMedia>>(emptyList())
    private val _isInitialLoading = MutableStateFlow(
        mediaStoreRepository.checkPermissionState() != MediaPermissionState.DENIED
    )
    val isInitialLoading: StateFlow<Boolean> = _isInitialLoading.asStateFlow()

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _gridColumnsOverride = MutableStateFlow<Int?>(null)
    val gridColumns: StateFlow<Int> = combine(settings, _gridColumnsOverride) { appSettings, override ->
        override ?: appSettings.gridColumns
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)

    private val favoriteIds: StateFlow<Set<Long>> = vaultDao.observeFavoriteIds()
        .map { it.toSet() }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val allMedia: StateFlow<List<GalleryMedia>> = combine(_rawMedia, favoriteIds) { media, favs ->
        if (favs.isEmpty()) media
        else media.map { item ->
            val isFav = favs.contains(item.id)
            if (item.isFavorite == isFav) item else item.copy(isFavorite = isFav)
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val photoSections: StateFlow<List<DateSection<MediaItemRef.Gallery>>> = allMedia
        .map { list -> DateGroupingUtils.groupGalleryByDate(list) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _photoFilter = MutableStateFlow("ALL")
    val photoFilter: StateFlow<String> = _photoFilter.asStateFlow()

    val filteredPhotoSections: StateFlow<List<DateSection<MediaItemRef.Gallery>>> = combine(
        photoSections,
        _photoFilter
    ) { sections, filter ->
        when {
            filter == "ALL" -> sections
            filter == "IMAGES" -> sections.mapNotNull { sec ->
                val list = sec.items.filter { it.item.mediaType == MediaType.IMAGE }
                if (list.isEmpty()) null else sec.copy(items = list)
            }
            filter == "VIDEOS" -> sections.mapNotNull { sec ->
                val list = sec.items.filter { it.item.mediaType == MediaType.VIDEO }
                if (list.isEmpty()) null else sec.copy(items = list)
            }
            filter.startsWith("ALBUM_") -> {
                val bucketId = filter.removePrefix("ALBUM_").toLongOrNull()
                if (bucketId == null) sections
                else sections.mapNotNull { sec ->
                    val list = sec.items.filter { it.item.bucketId == bucketId }
                    if (list.isEmpty()) null else sec.copy(items = list)
                }
            }
            else -> sections
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albums: StateFlow<List<AlbumFolder>> = _rawMedia
        .map { list ->
            if (list.isEmpty()) return@map emptyList()
            val byBucket = HashMap<Long, MutableList<GalleryMedia>>(32)
            for (item in list) {
                byBucket.getOrPut(item.bucketId) { mutableListOf() }.add(item)
            }
            byBucket.entries.map { (bucketId, items) ->
                var latestItem = items[0]
                var latestDate = latestItem.effectiveDateMs
                var images = 0
                var videos = 0
                for (item in items) {
                    if (item.mediaType == MediaType.IMAGE) images++ else videos++
                    val ts = item.effectiveDateMs
                    if (ts > latestDate) {
                        latestDate = ts
                        latestItem = item
                    }
                }
                AlbumFolder(
                    bucketId = bucketId,
                    name = latestItem.bucketName,
                    coverUri = latestItem.uri,
                    itemCount = items.size,
                    imageCount = images,
                    videoCount = videos,
                    latestDateMs = latestDate,
                    previewUris = items.take(3).map { it.uri }
                )
            }.sortedByDescending { it.latestDateMs }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<GalleryMedia>> = allMedia
        .map { list -> list.filter { it.isFavorite } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteSections: StateFlow<List<DateSection<MediaItemRef.Gallery>>> = favorites
        .map { list -> DateGroupingUtils.groupGalleryByDate(list) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val videos: StateFlow<List<GalleryMedia>> = allMedia
        .map { list -> list.filter { it.mediaType == MediaType.VIDEO } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val videoSections: StateFlow<List<DateSection<MediaItemRef.Gallery>>> = videos
        .map { list -> DateGroupingUtils.groupGalleryByDate(list) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _trashedMedia = MutableStateFlow<List<GalleryMedia>>(emptyList())
    val trashedMedia: StateFlow<List<GalleryMedia>> = _trashedMedia.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchFilter = MutableStateFlow(SearchFilterChip.ALL)
    val searchFilter: StateFlow<SearchFilterChip> = _searchFilter.asStateFlow()

    val searchResults: StateFlow<List<GalleryMedia>> = combine(
        allMedia,
        _searchQuery,
        _searchFilter
    ) { media, query, filter ->
        MediaSearchMatcher.filter(media = media, query = query, filter = filter)
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _dynamicAccent = MutableStateFlow<Color?>(null)
    val dynamicAccent: StateFlow<Color?> = _dynamicAccent.asStateFlow()

    private val _pendingMediaRequest = MutableSharedFlow<PendingMediaRequest>()
    val pendingMediaRequest = _pendingMediaRequest.asSharedFlow()

    private val _messages = MutableSharedFlow<String>()
    val messages = _messages.asSharedFlow()

    val hasMoreMedia: StateFlow<Boolean> = mediaStoreRepository.hasMoreMedia

    private var hasInitializedResume = false
    private var accentJob: Job? = null

    init {
        viewModelScope.launch {
            mediaStoreRepository.observeGalleryMedia().collect { items ->
                _rawMedia.value = items
                _isInitialLoading.value = false
            }
        }
    }

    fun loadNextMediaPage() {
        mediaStoreRepository.requestLoadNextPage()
    }

    fun ensureAllMediaLoaded() {
        mediaStoreRepository.requestLoadAllRemaining()
    }

    fun requiredPermissions(): Array<String> = mediaStoreRepository.requiredPermissions()

    fun onAppResumed() {
        val prevState = _permissionState.value
        val newState = mediaStoreRepository.checkPermissionState()
        _permissionState.value = newState
        if (newState == MediaPermissionState.DENIED) {
            _rawMedia.value = emptyList()
            _isInitialLoading.value = false
            return
        }
        if (!hasInitializedResume) {
            hasInitializedResume = true
            if (prevState == MediaPermissionState.DENIED) {
                _isInitialLoading.value = true
                mediaStoreRepository.requestRefresh()
            }
            return
        }
        mediaStoreRepository.requestRefreshIfChanged()
    }

    fun refreshPermissionsAndMedia() {
        val prevState = _permissionState.value
        val newState = mediaStoreRepository.checkPermissionState()
        _permissionState.value = newState
        if (newState == MediaPermissionState.DENIED) {
            _rawMedia.value = emptyList()
            _isInitialLoading.value = false
            return
        }
        if (prevState == MediaPermissionState.DENIED) {
            _isInitialLoading.value = true
        }
        mediaStoreRepository.requestRefresh()
    }

    fun selectTab(tab: GalleryTab) {
        _currentTab.value = tab
        clearSelection()
        if (tab != GalleryTab.PHOTOS) {
            ensureAllMediaLoaded()
        }
    }

    fun selectPhotoFilter(filter: String) {
        _photoFilter.value = filter
        if (filter != "ALL") {
            ensureAllMediaLoaded()
        }
    }

    fun updatePinchGridColumns(columns: Int) {
        val clamped = columns.coerceIn(2, 5)
        _gridColumnsOverride.value = clamped
        viewModelScope.launch {
            settingsRepository.setGridColumns(clamped)
        }
    }

    fun toggleSelection(id: Long) {
        _selectedIds.value = _selectedIds.value.let { current ->
            if (current.contains(id)) current - id else current + id
        }
    }

    fun setSelection(id: Long, selected: Boolean) {
        _selectedIds.value = if (selected) _selectedIds.value + id else _selectedIds.value - id
    }

    fun selectAll(ids: List<Long>) {
        _selectedIds.value = ids.toSet()
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun toggleFavorite(item: GalleryMedia) {
        viewModelScope.launch {
            if (item.isFavorite) {
                vaultDao.removeFavorite(item.id)
            } else {
                vaultDao.addFavorite(FavoriteEntity(mediaStoreId = item.id))
            }
        }
    }

    fun favoriteSelected() {
        val ids = _selectedIds.value
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val all = allMedia.value.filter { ids.contains(it.id) }
            val allAlreadyFav = all.all { it.isFavorite }
            for (item in all) {
                if (allAlreadyFav) {
                    vaultDao.removeFavorite(item.id)
                } else {
                    vaultDao.addFavorite(FavoriteEntity(mediaStoreId = item.id))
                }
            }
            clearSelection()
        }
    }

    fun requestTrash(items: List<GalleryMedia>) {
        if (items.isEmpty()) return
        val uris = items.map { it.uri }
        viewModelScope.launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intent = mediaStoreRepository.createTrashRequest(uris, true)
                if (intent != null) {
                    _pendingMediaRequest.emit(PendingMediaRequest.Trash(intent))
                }
            } else {
                val count = mediaStoreRepository.deleteDirectLegacy(uris)
                if (count > 0) {
                    _messages.emit("Deleted $count item(s)")
                    refreshPermissionsAndMedia()
                }
            }
            clearSelection()
        }
    }

    fun loadTrash() {
        viewModelScope.launch {
            _trashedMedia.value = mediaStoreRepository.loadTrashedMedia()
        }
    }

    fun requestRestoreFromTrash(items: List<GalleryMedia>) {
        if (items.isEmpty()) return
        val uris = items.map { it.uri }
        viewModelScope.launch {
            val intent = mediaStoreRepository.createTrashRequest(uris, false)
            if (intent != null) {
                _pendingMediaRequest.emit(PendingMediaRequest.RestoreFromTrash(intent))
            }
        }
    }

    fun requestPermanentDelete(items: List<GalleryMedia>) {
        if (items.isEmpty()) return
        val uris = items.map { it.uri }
        viewModelScope.launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intent = mediaStoreRepository.createDeleteRequest(uris)
                if (intent != null) {
                    _pendingMediaRequest.emit(PendingMediaRequest.PermanentDelete(intent))
                }
            } else {
                mediaStoreRepository.deleteDirectLegacy(uris)
                loadTrash()
                refreshPermissionsAndMedia()
            }
        }
    }

    fun renameItem(item: GalleryMedia, newName: String) {
        val sanitized = newName.trim()
        if (sanitized.isEmpty()) return
        val ext = item.displayName.substringAfterLast('.', "")
        val finalName = if (ext.isNotEmpty() && !sanitized.contains('.')) "$sanitized.$ext" else sanitized

        viewModelScope.launch {
            val success = mediaStoreRepository.renameMedia(item.uri, finalName)
            if (success) {
                _messages.emit("Renamed to $finalName")
                refreshPermissionsAndMedia()
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val writeIntent = mediaStoreRepository.createWriteRequest(listOf(item.uri))
                if (writeIntent != null) {
                    _pendingMediaRequest.emit(
                        PendingMediaRequest.RenameWrite(writeIntent, item.uri, finalName)
                    )
                } else {
                    _messages.emit("Unable to rename this item")
                }
            } else {
                _messages.emit("Unable to rename this item")
            }
        }
    }

    fun completePendingRename(uri: Uri, newName: String) {
        viewModelScope.launch {
            if (mediaStoreRepository.renameMedia(uri, newName)) {
                _messages.emit("Renamed to $newName")
                refreshPermissionsAndMedia()
            } else {
                _messages.emit("Rename failed")
            }
        }
    }

    fun setAsWallpaper(item: GalleryMedia, flags: Int) {
        viewModelScope.launch {
            val ok = mediaStoreRepository.setWallpaper(item.uri, flags)
            _messages.emit(if (ok) "Wallpaper updated" else "Could not set wallpaper")
        }
    }

    suspend fun getDetails(item: GalleryMedia): MediaDetails =
        mediaStoreRepository.getMediaDetails(item)

    fun saveEditedBitmap(bitmap: Bitmap, originalName: String, onSaved: (Uri?) -> Unit) {
        viewModelScope.launch {
            val uri = mediaStoreRepository.saveEditedBitmapToGallery(bitmap, originalName)
            if (uri != null) {
                _messages.emit("Saved edited copy to Pictures/Piktures")
                refreshPermissionsAndMedia()
            } else {
                _messages.emit("Failed to save edited image")
            }
            onSaved(uri)
        }
    }

    fun overwriteEditedBitmap(
        targetUri: Uri,
        bitmap: Bitmap,
        onResult: (MediaStoreRepository.OverwriteImageResult) -> Unit
    ) {
        viewModelScope.launch {
            val result = mediaStoreRepository.overwriteEditedBitmapToUri(targetUri, bitmap)
            when (result) {
                is MediaStoreRepository.OverwriteImageResult.Success -> {
                    _messages.emit("Original photo updated")
                    refreshPermissionsAndMedia()
                }
                is MediaStoreRepository.OverwriteImageResult.NeedsWritePermission -> Unit
                is MediaStoreRepository.OverwriteImageResult.Failure -> {
                    _messages.emit("Failed to overwrite original image")
                }
            }
            onResult(result)
        }
    }

    fun overwriteEditedFile(
        targetUri: Uri,
        sourceFile: File,
        onResult: (MediaStoreRepository.OverwriteImageResult) -> Unit
    ) {
        viewModelScope.launch {
            val result = mediaStoreRepository.overwriteEditedFileToUri(targetUri, sourceFile)
            when (result) {
                is MediaStoreRepository.OverwriteImageResult.Success -> {
                    _messages.emit("Original photo updated")
                    refreshPermissionsAndMedia()
                }
                is MediaStoreRepository.OverwriteImageResult.NeedsWritePermission -> Unit
                is MediaStoreRepository.OverwriteImageResult.Failure -> {
                    _messages.emit("Failed to overwrite original image")
                }
            }
            onResult(result)
        }
    }

    fun publishExportedVideoFile(
        file: File,
        originalName: String,
        onSaved: (Uri?) -> Unit
    ) {
        viewModelScope.launch {
            val uri = mediaStoreRepository.saveVideoFileToGallery(file, originalName)
            if (uri != null) {
                _messages.emit("Exported video saved to Movies/Piktures")
                refreshPermissionsAndMedia()
            } else {
                _messages.emit("Failed to save exported video")
            }
            onSaved(uri)
        }
    }

    fun saveTrimmedVideo(
        sourceUri: Uri,
        originalName: String,
        startMs: Long,
        endMs: Long,
        keepAudio: Boolean,
        onSaved: (Uri?) -> Unit
    ) {
        viewModelScope.launch {
            val uri = mediaStoreRepository.saveTrimmedVideoToGallery(
                sourceUri = sourceUri,
                originalName = originalName,
                startMs = startMs,
                endMs = endMs,
                keepAudio = keepAudio
            )
            if (uri != null) {
                _messages.emit("Saved trimmed video to Movies/Piktures")
                refreshPermissionsAndMedia()
            } else {
                _messages.emit("Failed to trim video")
            }
            onSaved(uri)
        }
    }

    fun updateDynamicAccentForMedia(item: GalleryMedia?) {
        accentJob?.cancel()
        if (item == null || item.mediaType != MediaType.IMAGE) {
            _dynamicAccent.value = null
            return
        }
        accentJob = viewModelScope.launch {
            delay(220L)
            val extracted = withContext(Dispatchers.IO) {
                try {
                    val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        runCatching {
                            context.contentResolver.loadThumbnail(item.uri, android.util.Size(96, 96), null)
                        }.getOrNull()
                    } else {
                        null
                    } ?: context.contentResolver.openInputStream(item.uri)?.use { stream ->
                        val opts = BitmapFactory.Options().apply { inSampleSize = 16 }
                        BitmapFactory.decodeStream(stream, null, opts)
                    } ?: return@withContext null

                    val palette = Palette.from(bmp).maximumColorCount(12).generate()
                    bmp.recycle()
                    val swatch = palette.vibrantSwatch
                        ?: palette.lightVibrantSwatch
                        ?: palette.dominantSwatch
                    swatch?.rgb?.let { Color(it) }
                } catch (_: Throwable) {
                    null
                }
            }
            _dynamicAccent.value = extracted
        }
    }

    fun clearDynamicAccent() {
        _dynamicAccent.value = null
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSearchFilter(chip: SearchFilterChip) {
        _searchFilter.value = chip
    }

    fun resetSearch() {
        _searchQuery.value = ""
        _searchFilter.value = SearchFilterChip.ALL
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setUiColorPalette(palette: UiColorPalette) {
        viewModelScope.launch { settingsRepository.setUiColorPalette(palette) }
    }

    fun setDefaultGridColumns(columns: Int) {
        _gridColumnsOverride.value = null
        viewModelScope.launch { settingsRepository.setGridColumns(columns) }
    }

    fun setRelockTimeout(timeout: RelockTimeout) {
        viewModelScope.launch { settingsRepository.setRelockTimeout(timeout) }
    }

    fun setReduceTransparency(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setReduceTransparency(enabled) }
    }

    fun emitMessage(message: String) {
        viewModelScope.launch { _messages.emit(message) }
    }
}

internal object MediaSearchMatcher {
    private val WHITESPACE_REGEX = Regex("\\s+")
    private val IMAGE_SYNONYMS = listOf("photos", "images", "pictures", "pics", "img")
    private val VIDEO_SYNONYMS = listOf("videos", "movies", "clips", "reels")
    private val FAVORITE_SYNONYMS = listOf("favorites", "favourites", "favs", "liked", "starred")

    fun filter(
        media: List<GalleryMedia>,
        query: String,
        filter: SearchFilterChip,
        nowMs: Long = System.currentTimeMillis()
    ): List<GalleryMedia> {
        val filteredByType = when (filter) {
            SearchFilterChip.ALL -> media
            SearchFilterChip.PHOTOS -> media.filter { it.mediaType == MediaType.IMAGE }
            SearchFilterChip.VIDEOS -> media.filter { it.mediaType == MediaType.VIDEO }
            SearchFilterChip.FAVORITES -> media.filter { it.isFavorite }
        }
        val locale = Locale.getDefault()
        val tokens = query.trim()
            .lowercase(locale)
            .split(WHITESPACE_REGEX)
            .filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return filteredByType

        val dateMatcher = DateGroupingUtils.FastDateMatcher(nowMs)
        return filteredByType.filter { item ->
            matchesItem(
                displayName = item.displayName,
                bucketName = item.bucketName,
                relativePath = item.relativePath,
                mimeType = item.mimeType,
                mediaType = item.mediaType,
                isFavorite = item.isFavorite,
                effectiveDateMs = item.effectiveDateMs,
                tokens = tokens,
                dateMatcher = dateMatcher,
                locale = locale
            )
        }
    }

    internal fun matchesItem(
        displayName: String,
        bucketName: String,
        relativePath: String,
        mimeType: String,
        mediaType: MediaType,
        isFavorite: Boolean,
        effectiveDateMs: Long,
        tokens: List<String>,
        dateMatcher: DateGroupingUtils.FastDateMatcher,
        locale: Locale = Locale.getDefault()
    ): Boolean {
        if (tokens.isEmpty()) return true
        val nameLower = displayName.lowercase(locale)
        val bucketLower = bucketName.lowercase(locale)
        val pathLower = relativePath.lowercase(locale)
        val mimeLower = mimeType.lowercase(locale)

        return tokens.all { token ->
            nameLower.contains(token) ||
                bucketLower.contains(token) ||
                pathLower.contains(token) ||
                mimeLower.contains(token) ||
                matchesFormatAlias(mimeLower, nameLower, token) ||
                matchesMediaTypeSynonym(mediaType, isFavorite, token) ||
                dateMatcher.matches(effectiveDateMs, token)
        }
    }

    private fun matchesFormatAlias(mimeLower: String, nameLower: String, token: String): Boolean {
        return when (token) {
            "jpg" -> mimeLower.contains("jpeg") || nameLower.endsWith(".jpeg")
            "jpeg" -> mimeLower.contains("jpeg") || nameLower.endsWith(".jpg")
            "heic" -> mimeLower.contains("heif") || nameLower.endsWith(".heif")
            "heif" -> mimeLower.contains("heic") || nameLower.endsWith(".heic")
            else -> false
        }
    }

    private fun matchesMediaTypeSynonym(
        mediaType: MediaType,
        isFavorite: Boolean,
        token: String
    ): Boolean {
        if (token.length < 3) return false
        val matchesType = when (mediaType) {
            MediaType.IMAGE -> IMAGE_SYNONYMS.any { it.startsWith(token) }
            MediaType.VIDEO -> VIDEO_SYNONYMS.any { it.startsWith(token) }
        }
        if (matchesType) return true
        return isFavorite && FAVORITE_SYNONYMS.any { it.startsWith(token) }
    }
}

package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.CloudSyncRepository
import com.example.domain.model.AppSettings
import com.example.domain.model.CloudUiState
import com.example.domain.model.GalleryMedia
import com.example.domain.model.SyncedMediaItem
import com.example.domain.model.UserCloudProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CloudSyncViewModel(
    private val repository: CloudSyncRepository,
    private val currentUserId: String
) : ViewModel() {

    val profileState: StateFlow<CloudUiState<UserCloudProfile?>> =
        repository.observeProfile(currentUserId)
            .map<UserCloudProfile?, CloudUiState<UserCloudProfile?>> { CloudUiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing cloud profile", rootError)
                emit(CloudUiState.Error(rootError.message ?: "Failed to load cloud profile"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = CloudUiState.Loading
            )

    val syncedItemsState: StateFlow<CloudUiState<List<SyncedMediaItem>>> =
        repository.observeSyncedItems(currentUserId)
            .map<List<SyncedMediaItem>, CloudUiState<List<SyncedMediaItem>>> { CloudUiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing synced media items", rootError)
                emit(CloudUiState.Error(rootError.message ?: "Failed to load synced items"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = CloudUiState.Loading
            )

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun syncPreferencesNow(
        displayName: String?,
        email: String?,
        settings: AppSettings
    ) {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            val result = repository.upsertProfile(currentUserId, displayName, email, settings)
            _isSyncing.value = false
            _statusBanner.value = result.fold(
                onSuccess = { "Preferences backed up to Firebase" },
                onFailure = { err -> err.localizedMessage ?: "Failed to sync preferences" }
            )
        }
    }

    fun backupFavoritesNow(favorites: List<GalleryMedia>) {
        if (_isSyncing.value) return
        if (favorites.isEmpty()) {
            _statusBanner.value = "Star photos or videos as Favorites first to back them up"
            return
        }
        viewModelScope.launch {
            _isSyncing.value = true
            val result = repository.syncFavoritesBatch(currentUserId, favorites)
            _isSyncing.value = false
            _statusBanner.value = result.fold(
                onSuccess = { count -> "Backed up $count favorite(s) to Firebase" },
                onFailure = { err -> err.localizedMessage ?: "Failed to back up favorites" }
            )
        }
    }

    fun saveItemNote(itemId: String, note: String) {
        viewModelScope.launch {
            val result = repository.updateItemNote(currentUserId, itemId, note)
            _statusBanner.value = result.fold(
                onSuccess = { "Note saved to cloud" },
                onFailure = { err -> err.localizedMessage ?: "Failed to save note" }
            )
        }
    }

    fun removeSyncedItem(itemId: String) {
        viewModelScope.launch {
            val result = repository.deleteSyncedItem(currentUserId, itemId)
            _statusBanner.value = result.fold(
                onSuccess = { "Removed item from cloud backup" },
                onFailure = { err -> err.localizedMessage ?: "Failed to remove item" }
            )
        }
    }

    private companion object {
        const val TAG = "CloudSyncVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}

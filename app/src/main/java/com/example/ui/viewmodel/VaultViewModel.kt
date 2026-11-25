package com.example.ui.viewmodel

import android.app.PendingIntent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SettingsRepository
import com.example.data.repository.MediaStoreRepository
import com.example.data.repository.StagedHideBatch
import com.example.data.repository.VaultImageRequest
import com.example.data.repository.VaultRepository
import com.example.domain.model.AppSettings
import com.example.domain.model.GalleryMedia
import com.example.domain.model.MediaItemRef
import com.example.domain.model.RelockTimeout
import com.example.domain.model.VaultMedia
import com.example.security.PassVerifyResult
import com.example.security.PasscodeManager
import com.example.security.VaultCryptoManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

sealed interface VaultHideEvent {
    data class NeedDeleteConfirmation(
        val pendingIntent: PendingIntent,
        val batch: StagedHideBatch
    ) : VaultHideEvent

    data class Completed(val count: Int) : VaultHideEvent
    data class Error(val message: String) : VaultHideEvent
}

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val mediaStoreRepository: MediaStoreRepository,
    private val passcodeManager: PasscodeManager,
    val cryptoManager: VaultCryptoManager,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _level1Unlocked = MutableStateFlow(false)
    val level1Unlocked: StateFlow<Boolean> = _level1Unlocked.asStateFlow()

    private val _level2Unlocked = MutableStateFlow(false)
    val level2Unlocked: StateFlow<Boolean> = _level2Unlocked.asStateFlow()

    private val _hasExtraPass = MutableStateFlow(false)
    val hasExtraPass: StateFlow<Boolean> = _hasExtraPass.asStateFlow()

    private val _lockoutRemainingSeconds = MutableStateFlow(0L)
    val lockoutRemainingSeconds: StateFlow<Long> = _lockoutRemainingSeconds.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _selectedVaultIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedVaultIds: StateFlow<Set<String>> = _selectedVaultIds.asStateFlow()

    val level1Items: StateFlow<List<VaultMedia>> = vaultRepository.observeLevelMedia(1)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val level2Items: StateFlow<List<VaultMedia>> = vaultRepository.observeLevelMedia(2)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val level1Sections: StateFlow<List<DateSection<MediaItemRef.Vault>>> = level1Items
        .map { list -> DateGroupingUtils.groupVaultByDate(list) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val level2Sections: StateFlow<List<DateSection<MediaItemRef.Vault>>> = level2Items
        .map { list -> DateGroupingUtils.groupVaultByDate(list) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _hideEvents = MutableSharedFlow<VaultHideEvent>()
    val hideEvents = _hideEvents.asSharedFlow()

    private val _vaultMessages = MutableSharedFlow<String>()
    val vaultMessages = _vaultMessages.asSharedFlow()

    private var backgroundedAtMs: Long = 0L
    private var lockoutTickerJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            vaultRepository.recoverIncompleteMoves()
            val hasPass = passcodeManager.hasExtraPass()
            val lockoutRem = passcodeManager.getLockoutRemainingSeconds()
            _hasExtraPass.value = hasPass
            _lockoutRemainingSeconds.value = lockoutRem
            if (lockoutRem > 0L) {
                startLockoutTickerIfNeeded()
            }
        }
    }

    fun onAppBackgrounded() {
        backgroundedAtMs = System.currentTimeMillis()
        if (settings.value.relockTimeout == RelockTimeout.IMMEDIATE) {
            lockAll()
        }
    }

    fun onAppForegrounded() {
        if (backgroundedAtMs > 0L) {
            val elapsed = System.currentTimeMillis() - backgroundedAtMs
            if (elapsed >= settings.value.relockTimeout.millis) {
                lockAll()
            }
            backgroundedAtMs = 0L
        }
        refreshPassState()
    }

    fun unlockLevel1() {
        _level1Unlocked.value = true
    }

    fun lockAll() {
        _level1Unlocked.value = false
        _level2Unlocked.value = false
        _selectedVaultIds.value = emptySet()
    }

    fun lockLevel2Only() {
        _level2Unlocked.value = false
        _selectedVaultIds.value = emptySet()
    }

    fun refreshPassState() {
        viewModelScope.launch(Dispatchers.IO) {
            val hasPass = passcodeManager.hasExtraPass()
            val rem = passcodeManager.getLockoutRemainingSeconds()
            _hasExtraPass.value = hasPass
            _lockoutRemainingSeconds.value = rem
            if (rem > 0L) {
                startLockoutTickerIfNeeded()
            }
        }
    }

    fun verifyLevel2Passcode(code: String, onResult: (PassVerifyResult) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                passcodeManager.verifyExtraPass(code)
            }
            when (result) {
                is PassVerifyResult.Success -> {
                    _level2Unlocked.value = true
                    _lockoutRemainingSeconds.value = 0L
                }
                is PassVerifyResult.LockedOut -> {
                    _lockoutRemainingSeconds.value = result.remainingSeconds
                    startLockoutTickerIfNeeded()
                }
                is PassVerifyResult.Failed -> Unit
            }
            onResult(result)
        }
    }

    fun setExtraPass(code: String) {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                passcodeManager.setExtraPass(code)
            }
            _hasExtraPass.value = true
            _vaultMessages.emit("Extra Secret Folder Pass saved")
        }
    }

    fun clearExtraPass() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                passcodeManager.clearExtraPass()
            }
            _hasExtraPass.value = false
            _level2Unlocked.value = false
            _vaultMessages.emit("Extra Secret Folder Pass removed")
        }
    }

    private fun startLockoutTickerIfNeeded() {
        lockoutTickerJob?.cancel()
        lockoutTickerJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                val rem = passcodeManager.getLockoutRemainingSeconds()
                _lockoutRemainingSeconds.value = rem
                if (rem <= 0L) break
                delay(1000L)
            }
        }
    }

    fun hideGalleryItems(items: List<GalleryMedia>) {
        if (items.isEmpty() || _isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val batch = vaultRepository.stageHideToLevel1(items)
                if (batch.vaultIds.isEmpty()) {
                    _hideEvents.emit(VaultHideEvent.Error("Could not encrypt selected media"))
                    return@launch
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val deleteIntent = mediaStoreRepository.createDeleteRequest(batch.sourceUris)
                    if (deleteIntent != null) {
                        _hideEvents.emit(VaultHideEvent.NeedDeleteConfirmation(deleteIntent, batch))
                    } else {
                        vaultRepository.rollbackHideBatch(batch.vaultIds)
                        _hideEvents.emit(VaultHideEvent.Error("Could not request original removal"))
                    }
                } else {
                    val deletedCount = mediaStoreRepository.deleteDirectLegacy(batch.sourceUris)
                    if (deletedCount > 0) {
                        vaultRepository.commitHideBatch(batch.vaultIds)
                        _hideEvents.emit(VaultHideEvent.Completed(batch.vaultIds.size))
                    } else {
                        vaultRepository.rollbackHideBatch(batch.vaultIds)
                        _hideEvents.emit(VaultHideEvent.Error("Unable to remove original files"))
                    }
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun onHideDeleteConfirmed(batch: StagedHideBatch, approved: Boolean) {
        viewModelScope.launch {
            if (approved) {
                vaultRepository.commitHideBatch(batch.vaultIds)
                _hideEvents.emit(VaultHideEvent.Completed(batch.vaultIds.size))
            } else {
                vaultRepository.rollbackHideBatch(batch.vaultIds)
                _hideEvents.emit(VaultHideEvent.Error("Move cancelled — originals untouched"))
            }
        }
    }

    fun moveToDoubleHiddenFolder(vaultId: String, onMoved: (Boolean) -> Unit) {
        viewModelScope.launch {
            val hasPass = withContext(Dispatchers.IO) { passcodeManager.hasExtraPass() }
            if (!hasPass) {
                _vaultMessages.emit("Set an Extra Secret Folder Pass in Settings first")
                onMoved(false)
                return@launch
            }
            _isBusy.value = true
            val ok = vaultRepository.moveToDoubleHiddenFolder(vaultId)
            _isBusy.value = false
            _vaultMessages.emit(
                if (ok) "Moved to Double Hidden Folder" else "Failed to move item"
            )
            onMoved(ok)
        }
    }

    fun unhideItems(vaultIds: List<String>, onDone: () -> Unit = {}) {
        if (vaultIds.isEmpty()) return
        viewModelScope.launch {
            _isBusy.value = true
            var restored = 0
            for (id in vaultIds) {
                if (vaultRepository.unhideAndRestore(id)) restored++
            }
            _isBusy.value = false
            _selectedVaultIds.value = emptySet()
            _vaultMessages.emit(
                if (restored > 0) "Restored $restored item(s) to gallery" else "Restore failed"
            )
            onDone()
        }
    }

    fun deleteFromVaultPermanently(vaultIds: List<String>, onDone: () -> Unit = {}) {
        if (vaultIds.isEmpty()) return
        viewModelScope.launch {
            _isBusy.value = true
            vaultRepository.deletePermanentlyFromVault(vaultIds)
            _isBusy.value = false
            _selectedVaultIds.value = emptySet()
            _vaultMessages.emit("Permanently wiped ${vaultIds.size} item(s)")
            onDone()
        }
    }

    fun toggleVaultSelection(id: String) {
        _selectedVaultIds.value = _selectedVaultIds.value.let { current ->
            if (current.contains(id)) current - id else current + id
        }
    }

    fun setVaultSelection(id: String, selected: Boolean) {
        _selectedVaultIds.value = if (selected) _selectedVaultIds.value + id else _selectedVaultIds.value - id
    }

    fun selectAllVault(ids: List<String>) {
        _selectedVaultIds.value = ids.toSet()
    }

    fun clearVaultSelection() {
        _selectedVaultIds.value = emptySet()
    }

    fun thumbnailRequest(item: VaultMedia): VaultImageRequest {
        val thumbFile = vaultRepository.getEncryptedFile(item.level, item.encryptedThumbName)
        val target = if (thumbFile.exists()) thumbFile else getVaultEncryptedFile(item)
        return VaultImageRequest(
            file = target,
            mimeType = "image/jpeg",
            isThumbnail = thumbFile.exists()
        )
    }

    fun fullImageRequest(item: VaultMedia): VaultImageRequest {
        return VaultImageRequest(
            file = getVaultEncryptedFile(item),
            mimeType = item.mimeType,
            isThumbnail = false
        )
    }

    fun getVaultEncryptedFile(item: VaultMedia): File =
        vaultRepository.getEncryptedFile(item.level, item.encryptedFileName)
}

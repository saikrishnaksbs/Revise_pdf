package com.revisepdf.app.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revisepdf.app.data.llm.ModelImportProgress
import com.revisepdf.app.data.llm.ModelKind
import com.revisepdf.app.data.llm.ModelRepository
import com.revisepdf.app.data.llm.ModelStatus
import com.revisepdf.app.data.prefs.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val notificationIntervalMinutes: Int = SettingsRepository.DEFAULT_INTERVAL_MINUTES,
    val notificationsEnabled: Boolean = true,
    val modelStatus: ModelStatus = ModelStatus(),
)

sealed interface ModelImportUiState {
    data object Idle : ModelImportUiState
    data class Copying(val bytesCopied: Long, val totalBytes: Long) : ModelImportUiState
    data class Failed(val message: String) : ModelImportUiState
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val modelRepository: ModelRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.notificationIntervalMinutes,
        settingsRepository.notificationsEnabled,
        modelRepository.status,
    ) { interval, enabled, model -> SettingsUiState(interval, enabled, model) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    private val _importState = MutableStateFlow<ModelImportUiState>(ModelImportUiState.Idle)
    val importState: StateFlow<ModelImportUiState> = _importState.asStateFlow()

    val availableIntervals: List<Int> = SettingsRepository.AVAILABLE_INTERVALS_MINUTES

    fun setIntervalMinutes(minutes: Int) {
        viewModelScope.launch { settingsRepository.setNotificationIntervalMinutes(minutes) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setNotificationsEnabled(enabled) }
    }

    fun importModelFile(uri: Uri, displayName: String, kind: ModelKind) {
        viewModelScope.launch {
            modelRepository.import(uri, displayName, kind).collect { progress ->
                _importState.value = when (progress) {
                    is ModelImportProgress.Copying -> ModelImportUiState.Copying(progress.bytesCopied, progress.totalBytes)
                    is ModelImportProgress.Failed -> ModelImportUiState.Failed(progress.message)
                    is ModelImportProgress.Done -> ModelImportUiState.Idle
                }
            }
        }
    }

    fun clearModel() {
        viewModelScope.launch { modelRepository.clear() }
    }
}

package com.revisepdf.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revisepdf.app.data.prefs.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val notificationIntervalMinutes: Int = SettingsRepository.DEFAULT_INTERVAL_MINUTES,
    val notificationsEnabled: Boolean = true,
)

class SettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.notificationIntervalMinutes,
        settingsRepository.notificationsEnabled,
    ) { interval, enabled -> SettingsUiState(interval, enabled) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    val availableIntervals: List<Int> = SettingsRepository.AVAILABLE_INTERVALS_MINUTES

    fun setIntervalMinutes(minutes: Int) {
        viewModelScope.launch { settingsRepository.setNotificationIntervalMinutes(minutes) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setNotificationsEnabled(enabled) }
    }
}

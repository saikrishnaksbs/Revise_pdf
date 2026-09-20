package com.revisepdf.app.session

import com.revisepdf.app.data.prefs.SettingsRepository
import com.revisepdf.app.notification.ReminderScheduler
import com.revisepdf.core.model.SessionState
import kotlinx.coroutines.flow.first

class RevisionSessionController(
    private val settingsRepository: SettingsRepository,
    private val reminderScheduler: ReminderScheduler,
) {
    suspend fun start(documentId: String) {
        settingsRepository.setActiveDocumentId(documentId)
        settingsRepository.setSessionState(SessionState.RUNNING)
        reminderScheduler.scheduleNext(settingsRepository.notificationIntervalMinutes.first())
    }

    suspend fun pause() {
        settingsRepository.setSessionState(SessionState.PAUSED)
        reminderScheduler.cancel()
    }

    suspend fun resume(documentId: String) = start(documentId)

    suspend fun stop() {
        settingsRepository.setSessionState(SessionState.STOPPED)
        settingsRepository.setActiveDocumentId(null)
        reminderScheduler.cancel()
    }

    fun canScheduleExactAlarms(): Boolean = reminderScheduler.canScheduleExactAlarms()
}

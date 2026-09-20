package com.revisepdf.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.revisepdf.app.PdfRevisionApplication
import com.revisepdf.core.model.SessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REMINDER_TICK) return
        val container = (context.applicationContext as PdfRevisionApplication).container
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sessionState = container.settingsRepository.sessionState.first()
                if (sessionState != SessionState.RUNNING) return@launch

                if (container.settingsRepository.notificationsEnabled.first()) {
                    container.notificationHelper.showReminder()
                }

                val interval = container.settingsRepository.notificationIntervalMinutes.first()
                container.reminderScheduler.scheduleNext(interval)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_REMINDER_TICK = "com.revisepdf.app.action.REMINDER_TICK"
    }
}

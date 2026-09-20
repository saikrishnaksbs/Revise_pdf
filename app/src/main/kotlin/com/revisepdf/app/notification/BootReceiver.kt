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

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val container = (context.applicationContext as PdfRevisionApplication).container
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (container.settingsRepository.sessionState.first() == SessionState.RUNNING) {
                    val interval = container.settingsRepository.notificationIntervalMinutes.first()
                    container.reminderScheduler.scheduleNext(interval)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

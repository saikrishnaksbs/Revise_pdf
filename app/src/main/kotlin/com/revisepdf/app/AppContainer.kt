package com.revisepdf.app

import android.content.Context
import com.revisepdf.app.data.db.AppDatabase
import com.revisepdf.app.data.llm.LlamaEngine
import com.revisepdf.app.data.llm.ModelRepository
import com.revisepdf.app.data.pdf.PageRenderer
import com.revisepdf.app.data.pdf.PdfProcessor
import com.revisepdf.app.data.prefs.SettingsRepository
import com.revisepdf.app.data.repository.LibraryRepository
import com.revisepdf.app.data.repository.QuestionGenerationRepository
import com.revisepdf.app.data.repository.RevisionRepository
import com.revisepdf.app.notification.NotificationHelper
import com.revisepdf.app.notification.ReminderScheduler
import com.revisepdf.app.session.RevisionSessionController

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AppDatabase by lazy { AppDatabase.getInstance(appContext) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }
    private val pdfProcessor: PdfProcessor by lazy { PdfProcessor(appContext) }
    val libraryRepository: LibraryRepository by lazy { LibraryRepository(appContext, database, pdfProcessor) }
    val revisionRepository: RevisionRepository by lazy { RevisionRepository(database) }
    val notificationHelper: NotificationHelper by lazy { NotificationHelper(appContext) }
    val reminderScheduler: ReminderScheduler by lazy { ReminderScheduler(appContext) }
    val sessionController: RevisionSessionController by lazy {
        RevisionSessionController(settingsRepository, reminderScheduler)
    }

    val modelRepository: ModelRepository by lazy { ModelRepository(appContext, settingsRepository) }
    private val llamaEngine: LlamaEngine by lazy { LlamaEngine() }
    private val pageRenderer: PageRenderer by lazy { PageRenderer() }
    val questionGenerationRepository: QuestionGenerationRepository by lazy {
        QuestionGenerationRepository(appContext, database, modelRepository, llamaEngine, pageRenderer)
    }
}

package com.revisepdf.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.revisepdf.app.AppContainer
import com.revisepdf.app.ui.importing.ImportViewModel
import com.revisepdf.app.ui.library.LibraryViewModel
import com.revisepdf.app.ui.revision.RevisionViewModel
import com.revisepdf.app.ui.settings.SettingsViewModel

class ViewModelFactory(
    private val container: AppContainer,
    private val documentId: String? = null,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        LibraryViewModel::class.java -> LibraryViewModel(container.libraryRepository) as T
        ImportViewModel::class.java -> ImportViewModel(container.libraryRepository) as T
        RevisionViewModel::class.java -> RevisionViewModel(
            documentId = requireNotNull(documentId) { "documentId is required for RevisionViewModel" },
            revisionRepository = container.revisionRepository,
            sessionController = container.sessionController,
            settingsRepository = container.settingsRepository,
            questionGenerationRepository = container.questionGenerationRepository,
            modelRepository = container.modelRepository,
        ) as T
        SettingsViewModel::class.java -> SettingsViewModel(
            container.settingsRepository,
            container.modelRepository,
        ) as T
        else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

package com.revisepdf.app.ui.importing

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revisepdf.app.data.repository.ImportProgress
import com.revisepdf.app.data.repository.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ImportUiState {
    data object Idle : ImportUiState
    data object Hashing : ImportUiState
    data class Processing(val pageIndex: Int, val totalPages: Int) : ImportUiState
    data object AlreadyProcessed : ImportUiState
    data class Done(val documentId: String) : ImportUiState
    data class Failed(val message: String) : ImportUiState
}

class ImportViewModel(private val libraryRepository: LibraryRepository) : ViewModel() {
    private val _state = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val state: StateFlow<ImportUiState> = _state.asStateFlow()

    fun importPdf(uri: Uri, displayName: String) {
        if (_state.value !is ImportUiState.Idle) return
        viewModelScope.launch {
            libraryRepository.importFromUri(uri, displayName).collect { progress ->
                _state.value = when (progress) {
                    is ImportProgress.Hashing -> ImportUiState.Hashing
                    is ImportProgress.AlreadyProcessed -> ImportUiState.AlreadyProcessed
                    is ImportProgress.ProcessingPage -> ImportUiState.Processing(progress.pageIndex, progress.totalPages)
                    is ImportProgress.Done -> ImportUiState.Done(progress.documentId)
                    is ImportProgress.Failed -> ImportUiState.Failed(progress.message)
                }
            }
        }
    }
}

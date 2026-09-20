package com.revisepdf.app.ui.revision

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revisepdf.app.data.db.RecallPointWithState
import com.revisepdf.app.data.prefs.SettingsRepository
import com.revisepdf.app.data.repository.RevisionRepository
import com.revisepdf.app.session.RevisionSessionController
import com.revisepdf.core.model.ReviewOutcome
import com.revisepdf.core.model.RevisionProgress
import com.revisepdf.core.model.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RevisionUiState(
    val queue: List<RecallPointWithState> = emptyList(),
    val currentIndex: Int = 0,
    val isAnswerRevealed: Boolean = false,
    val progress: RevisionProgress = RevisionProgress(null, 0, 0, 0),
    val sessionState: SessionState = SessionState.STOPPED,
    val isLoading: Boolean = true,
) {
    val current: RecallPointWithState? get() = queue.getOrNull(currentIndex)
    val isQueueExhausted: Boolean get() = !isLoading && queue.isNotEmpty() && current == null
}



class RevisionViewModel(
    private val documentId: String,
    private val revisionRepository: RevisionRepository,
    private val sessionController: RevisionSessionController,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RevisionUiState())
    val uiState: StateFlow<RevisionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { loadQueue() }
        viewModelScope.launch {
            revisionRepository.observeProgress(documentId).collect { progress ->
                _uiState.update { it.copy(progress = progress) }
            }
        }
        viewModelScope.launch {
            settingsRepository.sessionState.collect { state ->
                _uiState.update { it.copy(sessionState = state) }
            }
        }
    }

    private suspend fun loadQueue() {
        val due = revisionRepository.getDueRecallPoints(documentId)
        _uiState.update { it.copy(queue = due, currentIndex = 0, isAnswerRevealed = false, isLoading = false) }
    }

    fun refreshQueue() {
        viewModelScope.launch { loadQueue() }
    }

    fun reveal() {
        _uiState.update { it.copy(isAnswerRevealed = true) }
    }

    fun markOutcome(outcome: ReviewOutcome) {
        val current = _uiState.value.current ?: return
        viewModelScope.launch {
            revisionRepository.recordOutcome(current.id, outcome)
            _uiState.update { it.copy(currentIndex = it.currentIndex + 1, isAnswerRevealed = false) }
        }
    }

    fun start() {
        viewModelScope.launch { sessionController.start(documentId) }
    }

    fun pause() {
        viewModelScope.launch { sessionController.pause() }
    }

    fun resume() {
        viewModelScope.launch { sessionController.resume(documentId) }
    }

    fun stop() {
        viewModelScope.launch { sessionController.stop() }
    }

    fun canScheduleExactAlarms(): Boolean = sessionController.canScheduleExactAlarms()
}

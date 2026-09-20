package com.revisepdf.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revisepdf.app.data.repository.LibraryRepository
import com.revisepdf.core.model.DocumentRecord
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class LibraryViewModel(libraryRepository: LibraryRepository) : ViewModel() {
    val documents: StateFlow<List<DocumentRecord>> = libraryRepository.observeLibrary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

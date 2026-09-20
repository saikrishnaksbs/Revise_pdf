package com.revisepdf.app.ui.importing

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revisepdf.app.AppContainer
import com.revisepdf.app.ui.ViewModelFactory

@Composable
fun ImportScreen(
    container: AppContainer,
    uri: Uri,
    displayName: String,
    onDone: (String) -> Unit,
    onFailed: () -> Unit,
    viewModel: ImportViewModel = viewModel(factory = remember { ViewModelFactory(container) }),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(uri) {
        viewModel.importPdf(uri, displayName)
    }

    LaunchedEffect(state) {
        val current = state
        if (current is ImportUiState.Done) onDone(current.documentId)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Text(displayName, style = MaterialTheme.typography.titleMedium)
            when (val current = state) {
                is ImportUiState.Idle, ImportUiState.Hashing -> {
                    CircularProgressIndicator()
                    Text("Checking whether this PDF was already processed…")
                }
                is ImportUiState.AlreadyProcessed -> {
                    CircularProgressIndicator()
                    Text("Already processed before — loading from local storage…")
                }
                is ImportUiState.Processing -> {
                    val progress = if (current.totalPages > 0) {
                        (current.pageIndex + 1) / current.totalPages.toFloat()
                    } else {
                        0f
                    }
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    Text("Extracting page ${current.pageIndex + 1} of ${current.totalPages}")
                }
                is ImportUiState.Done -> {
                    CircularProgressIndicator()
                    Text("Done — opening revision…")
                }
                is ImportUiState.Failed -> {
                    Text("Couldn't process this PDF: ${current.message}", color = MaterialTheme.colorScheme.error)
                    LaunchedEffect(current) { onFailed() }
                }
            }
        }
    }
}

package com.revisepdf.app.ui.revision

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revisepdf.app.AppContainer
import com.revisepdf.app.ui.ViewModelFactory
import com.revisepdf.core.model.ReviewOutcome
import com.revisepdf.core.model.SessionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevisionScreen(
    container: AppContainer,
    documentId: String,
    onBack: () -> Unit,
    viewModel: RevisionViewModel = viewModel(
        factory = remember(documentId) { ViewModelFactory(container, documentId) },
    ),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Revise") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.sessionState != SessionState.STOPPED && !viewModel.canScheduleExactAlarms()) {
                ExactAlarmBanner()
            }
            ProgressSummary(state)
            SessionControls(
                sessionState = state.sessionState,
                onStart = viewModel::start,
                onPause = viewModel::pause,
                onResume = viewModel::resume,
                onStop = viewModel::stop,
            )
            val current = state.current
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                when {
                    state.isLoading -> CircularProgressIndicator()
                    current != null -> RecallCard(
                        prompt = current.prompt,
                        answer = current.answer,
                        isAnswerRevealed = state.isAnswerRevealed,
                        onReveal = viewModel::reveal,
                        onOutcome = viewModel::markOutcome,
                    )
                    else -> EmptyState(onRefresh = viewModel::refreshQueue)
                }
            }
        }
    }
}

@Composable
private fun ExactAlarmBanner() {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Reminders need the \"Alarms & reminders\" permission to fire on time on this Android version.")
            TextButton(onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")),
                    )
                }
            }) {
                Text("Grant permission")
            }
        }
    }
}

@Composable
private fun ProgressSummary(state: RevisionUiState) {
    val progress = state.progress
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("${progress.dueNow} due now · ${progress.reviewedAtLeastOnce}/${progress.totalRecallPoints} reviewed at least once")
        if (progress.totalRecallPoints > 0) {
            LinearProgressIndicator(
                progress = { progress.reviewedAtLeastOnce / progress.totalRecallPoints.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SessionControls(
    sessionState: SessionState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        when (sessionState) {
            SessionState.STOPPED -> Button(onClick = onStart) { Text("Start") }
            SessionState.RUNNING -> {
                OutlinedButton(onClick = onPause) { Text("Pause") }
                OutlinedButton(onClick = onStop) { Text("Stop") }
            }
            SessionState.PAUSED -> {
                Button(onClick = onResume) { Text("Resume") }
                OutlinedButton(onClick = onStop) { Text("Stop") }
            }
        }
    }
}

@Composable
private fun RecallCard(
    prompt: String,
    answer: String,
    isAnswerRevealed: Boolean,
    onReveal: () -> Unit,
    onOutcome: (ReviewOutcome) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(prompt, style = MaterialTheme.typography.titleMedium)
            if (isAnswerRevealed) {
                HorizontalDivider()
                Text(answer, style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onOutcome(ReviewOutcome.FORGOT) }) { Text("Forgot") }
                    Button(onClick = { onOutcome(ReviewOutcome.REMEMBERED) }) { Text("Remembered") }
                }
            } else {
                Button(onClick = onReveal) { Text("Reveal") }
            }
        }
    }
}

@Composable
private fun EmptyState(onRefresh: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Nothing due right now. New recall points appear here as they become due.")
        OutlinedButton(onClick = onRefresh) { Text("Check again") }
    }
}

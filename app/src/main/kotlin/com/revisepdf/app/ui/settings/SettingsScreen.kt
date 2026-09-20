package com.revisepdf.app.ui.settings

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import com.revisepdf.app.BuildConfig
import com.revisepdf.app.data.llm.ModelKind
import com.revisepdf.app.data.llm.ModelStatus
import com.revisepdf.app.ui.ViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    container: AppContainer,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = remember { ViewModelFactory(container) }),
) {
    val state by viewModel.uiState.collectAsState()
    val importState by viewModel.importState.collectAsState()
    val context = LocalContext.current

    val pickModel = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.importModelFile(uri, fileName(context, uri) ?: "model.gguf", ModelKind.MODEL)
        }
    }
    val pickMmproj = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.importModelFile(uri, fileName(context, uri) ?: "mmproj.gguf", ModelKind.MMPROJ)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Notify me during revision sessions")
                Switch(checked = state.notificationsEnabled, onCheckedChange = viewModel::setNotificationsEnabled)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            Text("Reminder interval")
            viewModel.availableIntervals.forEach { minutes ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = state.notificationIntervalMinutes == minutes,
                            onClick = { viewModel.setIntervalMinutes(minutes) },
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = state.notificationIntervalMinutes == minutes,
                        onClick = { viewModel.setIntervalMinutes(minutes) },
                    )
                    Text(formatInterval(minutes))
                }
            }
            if (BuildConfig.AI_ENABLED) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                AiModelSection(
                    status = state.modelStatus,
                    importState = importState,
                    onPickModel = { pickModel.launch(arrayOf("*/*")) },
                    onPickMmproj = { pickMmproj.launch(arrayOf("*/*")) },
                    onClear = viewModel::clearModel,
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            Text(
                "On some phones (including iQOO/vivo devices), reminders may be delayed unless " +
                    "you disable battery optimization for this app, allow it to auto-start, and " +
                    "grant \"Alarms & reminders\" access in system settings.",
            )
        }
    }
}

@Composable
private fun AiModelSection(
    status: ModelStatus,
    importState: ModelImportUiState,
    onPickModel: () -> Unit,
    onPickMmproj: () -> Unit,
    onClear: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("On-device AI questions", style = MaterialTheme.typography.titleMedium)
        Text(
            "Load a Qwen-VL GGUF model to turn paragraphs into real questions instead of " +
                "\"recall this paragraph\". Everything runs offline on this phone.",
            style = MaterialTheme.typography.bodySmall,
        )

        Text("Model: " + (status.modelName ?: "not loaded"))
        Text("Vision projector: " + (status.mmprojName ?: "not loaded (text-only)"))

        when (importState) {
            is ModelImportUiState.Copying -> {
                val copied = importState.bytesCopied / 1_000_000
                val total = importState.totalBytes / 1_000_000
                if (importState.totalBytes > 0) {
                    LinearProgressIndicator(
                        progress = { importState.bytesCopied.toFloat() / importState.totalBytes },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("Copying model… $copied MB of $total MB")
                } else {
                    Text("Copying model… $copied MB")
                }
            }
            is ModelImportUiState.Failed -> Text(importState.message, color = MaterialTheme.colorScheme.error)
            ModelImportUiState.Idle -> Unit
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onPickModel) { Text("Pick model") }
            OutlinedButton(onClick = onPickMmproj) { Text("Pick mmproj") }
        }
        if (status.isReady) {
            TextButton(onClick = onClear) { Text("Remove model") }
        }
    }
}

private fun fileName(context: Context, uri: Uri): String? {
    val cursor = context.contentResolver.query(uri, null, null, null, null) ?: return null
    return cursor.use {
        if (!it.moveToFirst()) return@use null
        val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0) it.getString(index) else null
    }
}

private fun formatInterval(minutes: Int): String = when {
    minutes < 60 -> "Every $minutes minute${if (minutes == 1) "" else "s"}"
    minutes % 60 == 0 -> "Every ${minutes / 60} hour${if (minutes / 60 == 1) "" else "s"}"
    else -> "Every $minutes minutes"
}

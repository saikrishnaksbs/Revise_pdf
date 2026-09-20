package com.revisepdf.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revisepdf.app.AppContainer
import com.revisepdf.app.ui.ViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    container: AppContainer,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = remember { ViewModelFactory(container) }),
) {
    val state by viewModel.uiState.collectAsState()

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
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
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
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            Text(
                "On some phones (including iQOO/vivo devices), reminders may be delayed unless " +
                    "you disable battery optimization for this app, allow it to auto-start, and " +
                    "grant \"Alarms & reminders\" access in system settings.",
            )
        }
    }
}

private fun formatInterval(minutes: Int): String = when {
    minutes < 60 -> "Every $minutes minute${if (minutes == 1) "" else "s"}"
    minutes % 60 == 0 -> "Every ${minutes / 60} hour${if (minutes / 60 == 1) "" else "s"}"
    else -> "Every $minutes minutes"
}

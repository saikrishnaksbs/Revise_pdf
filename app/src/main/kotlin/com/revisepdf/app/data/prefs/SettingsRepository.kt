package com.revisepdf.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.revisepdf.core.model.SessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val NOTIFICATION_INTERVAL_MINUTES = intPreferencesKey("notification_interval_minutes")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val SESSION_STATE = stringPreferencesKey("session_state")
        val ACTIVE_DOCUMENT_ID = stringPreferencesKey("active_document_id")
        val MODEL_PATH = stringPreferencesKey("model_path")
        val MODEL_NAME = stringPreferencesKey("model_name")
        val MMPROJ_PATH = stringPreferencesKey("mmproj_path")
        val MMPROJ_NAME = stringPreferencesKey("mmproj_name")
    }

    val notificationIntervalMinutes: Flow<Int> = context.dataStore.data.map {
        it[Keys.NOTIFICATION_INTERVAL_MINUTES] ?: DEFAULT_INTERVAL_MINUTES
    }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.NOTIFICATIONS_ENABLED] ?: true
    }

    val sessionState: Flow<SessionState> = context.dataStore.data.map { prefs ->
        prefs[Keys.SESSION_STATE]?.let { raw -> runCatching { SessionState.valueOf(raw) }.getOrNull() }
            ?: SessionState.STOPPED
    }

    val activeDocumentId: Flow<String?> = context.dataStore.data.map { it[Keys.ACTIVE_DOCUMENT_ID] }

    val modelPath: Flow<String?> = context.dataStore.data.map { it[Keys.MODEL_PATH] }
    val modelName: Flow<String?> = context.dataStore.data.map { it[Keys.MODEL_NAME] }
    val mmprojPath: Flow<String?> = context.dataStore.data.map { it[Keys.MMPROJ_PATH] }
    val mmprojName: Flow<String?> = context.dataStore.data.map { it[Keys.MMPROJ_NAME] }

    suspend fun setNotificationIntervalMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.NOTIFICATION_INTERVAL_MINUTES] = minutes }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setSessionState(state: SessionState) {
        context.dataStore.edit { it[Keys.SESSION_STATE] = state.name }
    }

    suspend fun setActiveDocumentId(documentId: String?) {
        context.dataStore.edit {
            if (documentId == null) it.remove(Keys.ACTIVE_DOCUMENT_ID) else it[Keys.ACTIVE_DOCUMENT_ID] = documentId
        }
    }

    suspend fun setModel(path: String, name: String) {
        context.dataStore.edit {
            it[Keys.MODEL_PATH] = path
            it[Keys.MODEL_NAME] = name
        }
    }

    suspend fun setMmproj(path: String, name: String) {
        context.dataStore.edit {
            it[Keys.MMPROJ_PATH] = path
            it[Keys.MMPROJ_NAME] = name
        }
    }

    suspend fun clearModel() {
        context.dataStore.edit {
            it.remove(Keys.MODEL_PATH)
            it.remove(Keys.MODEL_NAME)
            it.remove(Keys.MMPROJ_PATH)
            it.remove(Keys.MMPROJ_NAME)
        }
    }

    companion object {
        const val DEFAULT_INTERVAL_MINUTES = 30
        val AVAILABLE_INTERVALS_MINUTES = listOf(1, 5, 15, 30, 60, 120, 240)
    }
}

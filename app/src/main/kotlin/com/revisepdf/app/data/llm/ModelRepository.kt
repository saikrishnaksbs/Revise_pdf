package com.revisepdf.app.data.llm

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.revisepdf.app.data.prefs.SettingsRepository
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

enum class ModelKind { MODEL, MMPROJ }

sealed interface ModelImportProgress {
    data class Copying(val bytesCopied: Long, val totalBytes: Long) : ModelImportProgress
    data class Done(val path: String, val name: String) : ModelImportProgress
    data class Failed(val message: String) : ModelImportProgress
}

data class ModelStatus(
    val modelName: String? = null,
    val mmprojName: String? = null,
) {
    val isReady: Boolean get() = modelName != null
    val supportsVision: Boolean get() = isReady && mmprojName != null
}

class ModelRepository(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
) {
    private val modelDir: File
        get() = File(context.filesDir, "models").apply { mkdirs() }

    val status: Flow<ModelStatus> = combine(
        settingsRepository.modelName,
        settingsRepository.mmprojName,
    ) { model, mmproj -> ModelStatus(model, mmproj) }

    suspend fun modelPath(): String? = resolveExisting(settingsRepository.modelPath)

    suspend fun mmprojPath(): String? = resolveExisting(settingsRepository.mmprojPath)

    private suspend fun resolveExisting(source: Flow<String?>): String? =
        source.first()?.takeIf { File(it).exists() }

    fun import(uri: Uri, displayName: String, kind: ModelKind): Flow<ModelImportProgress> = flow {
        val totalBytes = querySize(uri)
        val target = File(modelDir, sanitize(displayName))

        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var copied = 0L
                    var sinceLastEmit = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        copied += read
                        sinceLastEmit += read
                        if (sinceLastEmit >= PROGRESS_EMIT_BYTES) {
                            emit(ModelImportProgress.Copying(copied, totalBytes))
                            sinceLastEmit = 0
                        }
                    }
                }
            } ?: run {
                emit(ModelImportProgress.Failed("Could not read the selected file."))
                return@flow
            }
        } catch (e: Exception) {
            target.delete()
            emit(ModelImportProgress.Failed(e.message ?: "Could not copy the model file."))
            return@flow
        }

        when (kind) {
            ModelKind.MODEL -> settingsRepository.setModel(target.absolutePath, displayName)
            ModelKind.MMPROJ -> settingsRepository.setMmproj(target.absolutePath, displayName)
        }
        emit(ModelImportProgress.Done(target.absolutePath, displayName))
    }.flowOn(Dispatchers.IO)

    suspend fun clear() {
        modelDir.listFiles()?.forEach { it.delete() }
        settingsRepository.clearModel()
    }

    private fun querySize(uri: Uri): Long {
        val cursor = context.contentResolver.query(uri, null, null, null, null) ?: return -1
        return cursor.use {
            if (!it.moveToFirst()) return@use -1
            val index = it.getColumnIndex(OpenableColumns.SIZE)
            if (index >= 0 && !it.isNull(index)) it.getLong(index) else -1
        }
    }

    private fun sanitize(name: String): String = name.replace(Regex("[^A-Za-z0-9._-]"), "_")

    private companion object {
        const val PROGRESS_EMIT_BYTES = 8L * 1024 * 1024
    }
}

package com.revisepdf.app.data.repository

import com.revisepdf.app.data.db.AppDatabase
import com.revisepdf.app.data.llm.LlamaEngine
import com.revisepdf.app.data.llm.ModelRepository
import com.revisepdf.core.ai.QuestionPrompt
import com.revisepdf.core.ai.QuestionResponseParser
import com.revisepdf.core.model.RecallType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

sealed interface GenerationProgress {
    data object LoadingModel : GenerationProgress
    data class Working(val done: Int, val total: Int) : GenerationProgress
    data class Finished(val generated: Int, val skipped: Int) : GenerationProgress
    data class Failed(val message: String) : GenerationProgress
}

class QuestionGenerationRepository(
    private val db: AppDatabase,
    private val modelRepository: ModelRepository,
    private val engine: LlamaEngine,
) {

    fun generateForDocument(documentId: String): Flow<GenerationProgress> = flow {
        val modelPath = modelRepository.modelPath()
        if (modelPath == null) {
            emit(GenerationProgress.Failed("Load a GGUF model in Settings first."))
            return@flow
        }

        emit(GenerationProgress.LoadingModel)
        try {
            engine.ensureLoaded(modelPath, modelRepository.mmprojPath())
        } catch (e: Exception) {
            emit(GenerationProgress.Failed(e.message ?: "Could not load the model."))
            return@flow
        }

        val pending = db.recallPointDao().getByType(
            documentId = documentId,
            type = RecallType.PARAGRAPH_RECALL.name,
            minLength = MIN_PARAGRAPH_LENGTH,
        )
        if (pending.isEmpty()) {
            emit(GenerationProgress.Finished(generated = 0, skipped = 0))
            return@flow
        }

        var generated = 0
        var skipped = 0
        emit(GenerationProgress.Working(0, pending.size))

        pending.forEachIndexed { index, item ->
            currentCoroutineContext().ensureActive()
            val parsed = runCatching {
                engine.generate(QuestionPrompt.SYSTEM, QuestionPrompt.forParagraph(item.paragraphText))
            }.getOrNull()?.let(QuestionResponseParser::parse)

            if (parsed == null) {
                skipped++
            } else {
                db.recallPointDao().updateGenerated(
                    id = item.recallPointId,
                    prompt = parsed.question,
                    answer = parsed.answer,
                    type = RecallType.QUESTION.name,
                )
                generated++
            }
            emit(GenerationProgress.Working(index + 1, pending.size))
        }

        emit(GenerationProgress.Finished(generated, skipped))
    }.flowOn(Dispatchers.Default)

    private companion object {
        // Below this a "paragraph" is usually a heading or caption, not worth a model round-trip
        // that costs seconds on-device.
        const val MIN_PARAGRAPH_LENGTH = 80
    }
}

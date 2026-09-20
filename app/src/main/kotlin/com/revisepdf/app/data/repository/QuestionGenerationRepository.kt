package com.revisepdf.app.data.repository

import android.content.Context
import com.revisepdf.app.data.db.AppDatabase
import com.revisepdf.app.data.db.ParagraphEntity
import com.revisepdf.app.data.db.RecallPointEntity
import com.revisepdf.app.data.db.toEntity
import com.revisepdf.app.data.llm.LlamaEngine
import com.revisepdf.app.data.llm.ModelRepository
import com.revisepdf.app.data.pdf.PageRenderer
import com.revisepdf.core.ai.QuestionPrompt
import com.revisepdf.core.ai.QuestionResponseParser
import com.revisepdf.core.model.RecallType
import com.revisepdf.core.schedule.ReviewScheduler
import java.io.File
import java.util.UUID
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
    private val context: Context,
    private val db: AppDatabase,
    private val modelRepository: ModelRepository,
    private val engine: LlamaEngine,
    private val pageRenderer: PageRenderer,
) {

    fun generateForDocument(documentId: String): Flow<GenerationProgress> = flow {
        val modelPath = modelRepository.modelPath()
        if (modelPath == null) {
            emit(GenerationProgress.Failed("Load a GGUF model in Settings first."))
            return@flow
        }
        val mmprojPath = modelRepository.mmprojPath()

        emit(GenerationProgress.LoadingModel)
        try {
            engine.ensureLoaded(modelPath, mmprojPath)
        } catch (e: Exception) {
            emit(GenerationProgress.Failed(e.message ?: "Could not load the model."))
            return@flow
        }

        val pendingParagraphs = db.recallPointDao().getByType(
            documentId = documentId,
            type = RecallType.PARAGRAPH_RECALL.name,
            minLength = MIN_PARAGRAPH_LENGTH,
        )

        // Pages that yielded no text at all are scans or full-page figures; those are exactly the
        // ones a vision model can turn into questions and the text pipeline cannot.
        val imagePages = if (mmprojPath == null) emptyList() else pagesWithoutText(documentId)

        val total = pendingParagraphs.size + imagePages.size
        if (total == 0) {
            emit(GenerationProgress.Finished(generated = 0, skipped = 0))
            return@flow
        }

        var generated = 0
        var skipped = 0
        var done = 0
        emit(GenerationProgress.Working(0, total))

        for (item in pendingParagraphs) {
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
            done++
            emit(GenerationProgress.Working(done, total))
        }

        if (imagePages.isNotEmpty()) {
            val pdfFile = pdfFileFor(documentId)
            for (pageIndex in imagePages) {
                currentCoroutineContext().ensureActive()
                val parsed = runCatching {
                    val image = pdfFile?.let { pageRenderer.renderPageToJpeg(it, pageIndex) }
                    image?.let {
                        engine.generateFromImage(
                            systemPrompt = QuestionPrompt.SYSTEM,
                            userPrompt = QuestionPrompt.forPageImage(pageIndex + 1),
                            imageBytes = it,
                        )
                    }
                }.getOrNull()?.let(QuestionResponseParser::parse)

                if (parsed == null) {
                    skipped++
                } else {
                    insertImageRecallPoint(documentId, pageIndex, parsed.question, parsed.answer)
                    generated++
                }
                done++
                emit(GenerationProgress.Working(done, total))
            }
        }

        emit(GenerationProgress.Finished(generated, skipped))
    }.flowOn(Dispatchers.Default)

    private suspend fun pagesWithoutText(documentId: String): List<Int> {
        val document = db.documentDao().getById(documentId) ?: return emptyList()
        val covered = db.paragraphDao().getPagesWithParagraphs(documentId).toSet()
        return (0 until document.totalPages).filterNot { it in covered }
    }

    private suspend fun pdfFileFor(documentId: String): File? {
        val document = db.documentDao().getById(documentId) ?: return null
        return File(File(context.filesDir, "pdfs"), "${document.contentHash}.pdf").takeIf { it.exists() }
    }

    private suspend fun insertImageRecallPoint(
        documentId: String,
        pageIndex: Int,
        question: String,
        answer: String,
    ) {
        // recall_points requires a parent paragraph row, so image-derived points get a stand-in
        // that records where they came from. It is never shown as source text.
        val paragraphId = UUID.randomUUID().toString()
        db.paragraphDao().insertAll(
            listOf(
                ParagraphEntity(
                    id = paragraphId,
                    documentId = documentId,
                    pageIndex = pageIndex,
                    orderInPage = IMAGE_PARAGRAPH_ORDER,
                    text = "Page ${pageIndex + 1} (image)",
                ),
            ),
        )

        val recallPointId = UUID.randomUUID().toString()
        db.recallPointDao().insertAll(
            listOf(
                RecallPointEntity(
                    id = recallPointId,
                    paragraphId = paragraphId,
                    documentId = documentId,
                    pageIndex = pageIndex,
                    type = RecallType.IMAGE_QUESTION.name,
                    prompt = question,
                    answer = answer,
                ),
            ),
        )

        db.revisionDao().insertReviewStates(
            listOf(ReviewScheduler.initialState(recallPointId, System.currentTimeMillis()).toEntity(documentId)),
        )
    }

    private companion object {
        // Below this a "paragraph" is usually a heading or caption, not worth a model round-trip
        // that costs seconds on-device.
        const val MIN_PARAGRAPH_LENGTH = 80
        const val IMAGE_PARAGRAPH_ORDER = -1
    }
}

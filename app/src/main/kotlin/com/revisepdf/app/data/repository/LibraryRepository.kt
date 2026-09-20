package com.revisepdf.app.data.repository

import android.content.Context
import android.net.Uri
import com.revisepdf.app.data.db.AppDatabase
import com.revisepdf.app.data.db.DocumentEntity
import com.revisepdf.app.data.db.ParagraphEntity
import com.revisepdf.app.data.db.RecallPointEntity
import com.revisepdf.app.data.db.ReviewStateEntity
import com.revisepdf.app.data.db.toDomain
import com.revisepdf.app.data.db.toEntity
import com.revisepdf.app.data.pdf.PdfProcessor
import com.revisepdf.core.hash.ContentHash
import com.revisepdf.core.model.DocumentRecord
import com.revisepdf.core.model.Paragraph
import com.revisepdf.core.recall.NaiveParagraphRecallGenerator
import com.revisepdf.core.schedule.ReviewScheduler
import com.revisepdf.core.text.ParagraphSplitter
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

sealed interface ImportProgress {
    data object Hashing : ImportProgress
    data object AlreadyProcessed : ImportProgress
    data class ProcessingPage(val pageIndex: Int, val totalPages: Int) : ImportProgress
    data class Done(val documentId: String) : ImportProgress
    data class Failed(val message: String) : ImportProgress
}

class LibraryRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val pdfProcessor: PdfProcessor,
) {
    private val pdfDir: File
        get() = File(context.filesDir, "pdfs").apply { mkdirs() }

    fun observeLibrary(): Flow<List<DocumentRecord>> =
        db.documentDao().observeAll().map { list -> list.map { it.toDomain() } }

    fun importFromUri(uri: Uri, displayName: String): Flow<ImportProgress> = callbackFlow {
        trySend(ImportProgress.Hashing)

        val bytes = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()

        if (bytes == null) {
            trySend(ImportProgress.Failed("Could not read the selected PDF."))
            close()
            return@callbackFlow
        }

        val hash = ContentHash.sha256(bytes)
        val existing = db.documentDao().findByContentHash(hash)
        if (existing != null && existing.isFullyProcessed) {
            trySend(ImportProgress.AlreadyProcessed)
            trySend(ImportProgress.Done(existing.id))
            close()
            return@callbackFlow
        }

        val documentId = existing?.id ?: UUID.randomUUID().toString()
        val file = File(pdfDir, "$hash.pdf")
        if (!file.exists()) file.writeBytes(bytes)

        try {
            val pageTexts = pdfProcessor.extractPages(file) { progress ->
                trySend(ImportProgress.ProcessingPage(progress.pageIndex, progress.totalPages))
            }

            db.documentDao().insert(
                DocumentEntity(
                    id = documentId,
                    contentHash = hash,
                    displayName = displayName,
                    importedAtEpochMillis = System.currentTimeMillis(),
                    totalPages = pageTexts.size,
                    isFullyProcessed = false,
                ),
            )

            val generator = NaiveParagraphRecallGenerator()
            val now = System.currentTimeMillis()
            val paragraphEntities = mutableListOf<ParagraphEntity>()
            val recallEntities = mutableListOf<RecallPointEntity>()
            val reviewEntities = mutableListOf<ReviewStateEntity>()

            pageTexts.forEachIndexed { pageIndex, pageText ->
                ParagraphSplitter.split(pageText).forEachIndexed { order, text ->
                    val paragraph = Paragraph(
                        id = UUID.randomUUID().toString(),
                        documentId = documentId,
                        pageIndex = pageIndex,
                        orderInPage = order,
                        text = text,
                    )
                    paragraphEntities += paragraph.toEntity()
                    val recallPoint = generator.generate(paragraph)
                    recallEntities += recallPoint.toEntity()
                    reviewEntities += ReviewScheduler.initialState(recallPoint.id, now).toEntity(documentId)
                }
            }

            db.paragraphDao().insertAll(paragraphEntities)
            db.recallPointDao().insertAll(recallEntities)
            db.revisionDao().insertReviewStates(reviewEntities)
            db.documentDao().setFullyProcessed(documentId, true)

            trySend(ImportProgress.Done(documentId))
        } catch (e: Exception) {
            trySend(ImportProgress.Failed(e.message ?: "Failed to process the PDF."))
        }
        close()
        awaitClose { }
    }.flowOn(Dispatchers.IO)
}

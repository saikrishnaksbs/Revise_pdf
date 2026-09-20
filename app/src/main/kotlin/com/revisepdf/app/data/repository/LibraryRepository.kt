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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
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

    // channelFlow with suspending send(), not callbackFlow with trySend(): on the default
    // rendezvous channel trySend drops the element whenever the collector is not already parked
    // on receive, so terminal events like Done could be lost and the import would appear to hang.
    fun importFromUri(uri: Uri, displayName: String): Flow<ImportProgress> = channelFlow {
        send(ImportProgress.Hashing)

        val staging = File(pdfDir, "staging-${UUID.randomUUID()}.tmp")
        val hash = try {
            context.contentResolver.openInputStream(uri).use { input ->
                if (input == null) null else staging.outputStream().use { ContentHash.copyAndHash(input, it) }
            }
        } catch (e: Exception) {
            staging.delete()
            send(ImportProgress.Failed(e.message ?: "Could not read the selected PDF."))
            return@channelFlow
        }

        if (hash == null) {
            staging.delete()
            send(ImportProgress.Failed("Could not open the selected PDF."))
            return@channelFlow
        }

        val existing = db.documentDao().findByContentHash(hash)
        if (existing != null && existing.isFullyProcessed) {
            staging.delete()
            send(ImportProgress.AlreadyProcessed)
            send(ImportProgress.Done(existing.id))
            return@channelFlow
        }

        val documentId = existing?.id ?: UUID.randomUUID().toString()
        val file = File(pdfDir, "$hash.pdf")
        if (file.exists()) staging.delete() else staging.renameTo(file)

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

            send(ImportProgress.Done(documentId))
        } catch (e: Exception) {
            send(ImportProgress.Failed(e.message ?: "Failed to process the PDF."))
        }
    }.buffer(Channel.UNLIMITED).flowOn(Dispatchers.IO)
}

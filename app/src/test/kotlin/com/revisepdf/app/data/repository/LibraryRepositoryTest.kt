package com.revisepdf.app.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.revisepdf.app.data.db.AppDatabase
import com.revisepdf.app.data.pdf.PdfProcessor
import java.io.ByteArrayInputStream
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LibraryRepositoryTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: LibraryRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LibraryRepository(context, db, PdfProcessor(context))
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun registerSamplePdf(uri: Uri) {
        val bytes = checkNotNull(javaClass.classLoader?.getResourceAsStream("sample.pdf")) {
            "sample.pdf fixture is missing from test resources"
        }.readBytes()
        shadowOf(context.contentResolver).registerInputStream(uri, ByteArrayInputStream(bytes))
    }

    @Test
    fun `importing a pdf extracts paragraphs and reports completion`() = runTest {
        val uri = Uri.parse("content://test/sample.pdf")
        registerSamplePdf(uri)

        val events = repository.importFromUri(uri, "sample.pdf").toList()

        // Regression: Done used to go through trySend on a rendezvous channel and could be
        // dropped, leaving the import screen waiting forever for a completion that had happened.
        val done = events.last()
        assertTrue("expected Done, got $done (all: $events)", done is ImportProgress.Done)
        assertTrue("expected per-page progress", events.any { it is ImportProgress.ProcessingPage })

        val documentId = (done as ImportProgress.Done).documentId
        val paragraphs = db.paragraphDao().getByDocument(documentId)
        assertTrue("expected extracted paragraphs, got ${paragraphs.size}", paragraphs.isNotEmpty())
        assertEquals(paragraphs.size, db.recallPointDao().countForDocument(documentId))

        val document = checkNotNull(db.documentDao().getById(documentId))
        assertTrue("document should be marked processed", document.isFullyProcessed)
        assertEquals(2, document.totalPages)
    }

    @Test
    fun `re-importing the same pdf reuses the stored extraction`() = runTest {
        val first = Uri.parse("content://test/first.pdf")
        registerSamplePdf(first)
        val firstEvents = repository.importFromUri(first, "sample.pdf").toList()
        val firstId = (firstEvents.last() as ImportProgress.Done).documentId

        val second = Uri.parse("content://test/second-copy.pdf")
        registerSamplePdf(second)
        val secondEvents = repository.importFromUri(second, "sample-copy.pdf").toList()

        assertTrue("expected the cached path", secondEvents.any { it is ImportProgress.AlreadyProcessed })
        assertEquals(firstId, (secondEvents.last() as ImportProgress.Done).documentId)
    }

    @Test
    fun `an unreadable uri reports failure rather than stalling`() = runTest {
        val events = repository.importFromUri(Uri.parse("content://test/missing.pdf"), "missing.pdf").toList()

        assertTrue("expected a Failed event, got $events", events.last() is ImportProgress.Failed)
    }
}

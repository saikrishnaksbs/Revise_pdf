package com.revisepdf.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(document: DocumentEntity)

    @Query("SELECT * FROM documents WHERE contentHash = :hash LIMIT 1")
    suspend fun findByContentHash(hash: String): DocumentEntity?

    @Query("SELECT * FROM documents ORDER BY importedAtEpochMillis DESC")
    fun observeAll(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getById(id: String): DocumentEntity?

    @Query("UPDATE documents SET isFullyProcessed = :isFullyProcessed WHERE id = :id")
    suspend fun setFullyProcessed(id: String, isFullyProcessed: Boolean)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface ParagraphDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(paragraphs: List<ParagraphEntity>)

    @Query("SELECT * FROM paragraphs WHERE documentId = :documentId ORDER BY pageIndex ASC, orderInPage ASC")
    suspend fun getByDocument(documentId: String): List<ParagraphEntity>

    @Query("SELECT DISTINCT pageIndex FROM paragraphs WHERE documentId = :documentId")
    suspend fun getPagesWithParagraphs(documentId: String): List<Int>
}

@Dao
interface RecallPointDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(points: List<RecallPointEntity>)

    @Query("SELECT COUNT(*) FROM recall_points WHERE documentId = :documentId")
    suspend fun countForDocument(documentId: String): Int

    @Query(
        """
        SELECT rp.id AS recallPointId, rp.pageIndex AS pageIndex, p.text AS paragraphText
        FROM recall_points rp
        JOIN paragraphs p ON p.id = rp.paragraphId
        WHERE rp.documentId = :documentId AND rp.type = :type AND LENGTH(p.text) >= :minLength
        ORDER BY rp.pageIndex ASC
        """,
    )
    suspend fun getByType(documentId: String, type: String, minLength: Int): List<ParagraphRecallPoint>

    @Query("UPDATE recall_points SET prompt = :prompt, answer = :answer, type = :type WHERE id = :id")
    suspend fun updateGenerated(id: String, prompt: String, answer: String, type: String)

    @Query("SELECT COUNT(*) FROM recall_points WHERE documentId = :documentId AND type = :type")
    fun observeCountByType(documentId: String, type: String): Flow<Int>
}

@Dao
interface RevisionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviewStates(states: List<ReviewStateEntity>)

    @Update
    suspend fun updateReviewState(state: ReviewStateEntity)

    @Query("SELECT * FROM review_states WHERE recallPointId = :id")
    suspend fun getReviewState(id: String): ReviewStateEntity?

    @Query(
        """
        SELECT rp.id AS id, rp.documentId AS documentId, rp.pageIndex AS pageIndex,
               rp.prompt AS prompt, rp.answer AS answer, rp.type AS type,
               p.text AS sourceText,
               rs.level AS level, rs.dueAtEpochMillis AS dueAtEpochMillis, rs.timesReviewed AS timesReviewed
        FROM recall_points rp
        JOIN review_states rs ON rs.recallPointId = rp.id
        JOIN paragraphs p ON p.id = rp.paragraphId
        WHERE rp.documentId = :documentId AND rs.dueAtEpochMillis <= :nowEpochMillis
        ORDER BY rs.dueAtEpochMillis ASC
        LIMIT :limit
        """,
    )
    suspend fun getDue(documentId: String, nowEpochMillis: Long, limit: Int): List<RecallPointWithState>

    @Query("SELECT COUNT(*) FROM review_states WHERE documentId = :documentId AND dueAtEpochMillis <= :nowEpochMillis")
    fun observeDueCount(documentId: String, nowEpochMillis: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM recall_points WHERE documentId = :documentId")
    fun observeTotalCount(documentId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM review_states WHERE documentId = :documentId AND timesReviewed > 0")
    fun observeReviewedCount(documentId: String): Flow<Int>
}

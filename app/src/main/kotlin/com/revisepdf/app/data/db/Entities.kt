package com.revisepdf.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "documents",
    indices = [Index(value = ["contentHash"], unique = true)],
)
data class DocumentEntity(
    @PrimaryKey val id: String,
    val contentHash: String,
    val displayName: String,
    val importedAtEpochMillis: Long,
    val totalPages: Int,
    val isFullyProcessed: Boolean,
)

@Entity(
    tableName = "paragraphs",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("documentId")],
)
data class ParagraphEntity(
    @PrimaryKey val id: String,
    val documentId: String,
    val pageIndex: Int,
    val orderInPage: Int,
    val text: String,
)

@Entity(
    tableName = "recall_points",
    foreignKeys = [
        ForeignKey(
            entity = ParagraphEntity::class,
            parentColumns = ["id"],
            childColumns = ["paragraphId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("paragraphId"), Index("documentId")],
)
data class RecallPointEntity(
    @PrimaryKey val id: String,
    val paragraphId: String,
    val documentId: String,
    val pageIndex: Int,
    val type: String,
    val prompt: String,
    val answer: String,
)

@Entity(
    tableName = "review_states",
    foreignKeys = [
        ForeignKey(
            entity = RecallPointEntity::class,
            parentColumns = ["id"],
            childColumns = ["recallPointId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("documentId"), Index("dueAtEpochMillis")],
)
data class ReviewStateEntity(
    @PrimaryKey val recallPointId: String,
    val documentId: String,
    val level: Int,
    val dueAtEpochMillis: Long,
    val lastReviewedAtEpochMillis: Long?,
    val timesReviewed: Int,
)

data class RecallPointWithState(
    val id: String,
    val documentId: String,
    val pageIndex: Int,
    val prompt: String,
    val answer: String,
    val level: Int,
    val dueAtEpochMillis: Long,
    val timesReviewed: Int,
)

package com.revisepdf.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        DocumentEntity::class,
        ParagraphEntity::class,
        RecallPointEntity::class,
        ReviewStateEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun paragraphDao(): ParagraphDao
    abstract fun recallPointDao(): RecallPointDao
    abstract fun revisionDao(): RevisionDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "revise_pdf.db",
            ).build().also { instance = it }
        }
    }
}

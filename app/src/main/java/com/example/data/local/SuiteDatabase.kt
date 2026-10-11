package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.ClipboardEntity
import com.example.data.model.DesktopSettingsEntity
import com.example.data.model.NoteEntity
import com.example.data.model.TerminalHistoryEntity

@Database(
    entities = [
        NoteEntity::class,
        TerminalHistoryEntity::class,
        ClipboardEntity::class,
        DesktopSettingsEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class SuiteDatabase : RoomDatabase() {
    abstract fun suiteDao(): SuiteDao

    companion object {
        @Volatile
        private var INSTANCE: SuiteDatabase? = null

        // Migration from version 1 to 2 - add any new columns/tables here
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Example: Add a new column if needed
                // database.execSQL("ALTER TABLE notes ADD COLUMN is_pinned INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): SuiteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SuiteDatabase::class.java,
                    "android_desktop_suite.db"
                )
                .addMigrations(MIGRATION_1_2)
                // Only use fallbackToDestructiveMigration in debug builds
                // .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

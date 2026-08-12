package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
    version = 1,
    exportSchema = false
)
abstract class SuiteDatabase : RoomDatabase() {
    abstract fun suiteDao(): SuiteDao

    companion object {
        @Volatile
        private var INSTANCE: SuiteDatabase? = null

        fun getDatabase(context: Context): SuiteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SuiteDatabase::class.java,
                    "android_desktop_suite.db"
                ).fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

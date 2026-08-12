package com.example.data.local

import androidx.room.*
import com.example.data.model.ClipboardEntity
import com.example.data.model.DesktopSettingsEntity
import com.example.data.model.NoteEntity
import com.example.data.model.TerminalHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SuiteDao {
    // Notes
    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: Long)

    // Terminal History
    @Query("SELECT * FROM terminal_history ORDER BY timestamp DESC LIMIT 100")
    fun getTerminalHistory(): Flow<List<TerminalHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTerminalHistory(entry: TerminalHistoryEntity)

    @Query("DELETE FROM terminal_history")
    suspend fun clearTerminalHistory()

    // Clipboard History
    @Query("SELECT * FROM clipboard_history ORDER BY timestamp DESC LIMIT 50")
    fun getClipboardHistory(): Flow<List<ClipboardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClipboard(entry: ClipboardEntity)

    // Desktop Settings
    @Query("SELECT * FROM desktop_settings WHERE id = 1")
    fun getSettings(): Flow<DesktopSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: DesktopSettingsEntity)
}

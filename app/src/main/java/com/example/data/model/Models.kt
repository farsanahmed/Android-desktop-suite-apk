package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val category: String = "General",
    val colorHex: String = "#0284C7",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "terminal_history")
data class TerminalHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val command: String,
    val output: String,
    val isError: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "clipboard_history")
data class ClipboardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "desktop_settings")
data class DesktopSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val isDarkMode: Boolean = true,
    val accentColorHex: String = "#0078D4", // Windows 11 Blue
    val transparency: Float = 0.85f,
    val wallpaperUri: String = "default",
    val adbHost: String = "127.0.0.1",
    val adbPort: Int = 5555
)

enum class DesktopApp(
    val title: String,
    val iconName: String,
    val category: String
) {
    DASHBOARD("Dashboard", "dashboard", "System"),
    DEVICE_MANAGER("Device Manager", "devices", "System"),
    ADB_FASTBOOT("ADB & Fastboot Studio", "terminal", "Developer"),
    FILE_EXPLORER("File Explorer", "folder", "System"),
    APK_MANAGER("APK Installer & Extractor", "android", "Developer"),
    REMOTE_DESKTOP("Screen Mirroring", "cast", "Tools"),
    AI_ASSISTANT("AI Assistant", "auto_awesome", "Intelligence"),
    MEDIA_PLAYER("Media Suite", "play_circle", "Entertainment"),
    UTILITIES("Utilities & Tools", "build", "Productivity"),
    SETTINGS("Settings", "settings", "System")
}

data class DeviceMetrics(
    val model: String,
    val manufacturer: String,
    val androidVersion: String,
    val sdkInt: Int,
    val batteryLevel: Int,
    val isCharging: Boolean,
    val batteryTempC: Float,
    val ramUsedMb: Long,
    val ramTotalMb: Long,
    val storageUsedGb: Float,
    val storageTotalGb: Float,
    val cpuUsagePercent: Int,
    val cpuCores: Int,
    val gpuModel: String,
    val serialNumber: String,
    val ipAddress: String,
    val macAddress: String,
    val isRooted: Boolean,
    val isBootloaderUnlocked: Boolean,
    val adbAuthorized: Boolean,
    val connectionType: String
)

data class FileItem(
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val isDirectory: Boolean,
    val lastModified: Long,
    val permissions: String = "rw-r--r--"
)

data class AppPackageInfo(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val sizeBytes: Long,
    val isSystemApp: Boolean,
    val targetSdk: Int,
    val isInstalled: Boolean = true
)

data class AiChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "user" or "ai"
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isCode: Boolean = false
)

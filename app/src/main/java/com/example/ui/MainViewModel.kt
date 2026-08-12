package com.example.ui

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.engine.AdbConnectionManager
import com.example.data.engine.AdbFastbootEngine
import com.example.data.engine.DeviceDiagnosticEngine
import com.example.data.engine.FileCompressionEngine
import com.example.data.engine.GeminiAssistantEngine
import com.example.data.engine.NetworkDiagnosticsEngine
import com.example.data.model.AiChatMessage
import com.example.data.model.AppPackageInfo
import com.example.data.model.ClipboardEntity
import com.example.data.model.DesktopApp
import com.example.data.model.DesktopSettingsEntity
import com.example.data.model.DeviceMetrics
import com.example.data.model.FileItem
import com.example.data.model.NoteEntity
import com.example.data.model.TerminalHistoryEntity
import com.example.data.local.SuiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = SuiteDatabase.getDatabase(application)
    private val suiteDao = db.suiteDao()

    val diagnosticEngine = DeviceDiagnosticEngine(application)
    val adbConnectionManager = AdbConnectionManager(application)
    val adbEngine = AdbFastbootEngine(adbConnectionManager)
    val fileEngine = FileCompressionEngine()
    val netEngine = NetworkDiagnosticsEngine()
    val aiEngine = GeminiAssistantEngine()

    // Desktop Environment State
    val openWindows = mutableStateListOf<DesktopApp>(DesktopApp.FILE_EXPLORER)
    val activeWindow = mutableStateOf<DesktopApp?>(DesktopApp.FILE_EXPLORER)
    val isStartMenuOpen = mutableStateOf(true)
    val isQuickSettingsOpen = mutableStateOf(false)
    val isWidgetsOpen = mutableStateOf(false)
    val searchQuery = mutableStateOf("")

    // Settings
    private val _settings = MutableStateFlow(DesktopSettingsEntity())
    val settings: StateFlow<DesktopSettingsEntity> = _settings.asStateFlow()

    // Metrics
    private val _deviceMetrics = MutableStateFlow<DeviceMetrics?>(null)
    val deviceMetrics: StateFlow<DeviceMetrics?> = _deviceMetrics.asStateFlow()

    // Notes
    private val _notes = MutableStateFlow<List<NoteEntity>>(emptyList())
    val notes: StateFlow<List<NoteEntity>> = _notes.asStateFlow()

    // Terminal History
    private val _terminalHistory = MutableStateFlow<List<TerminalHistoryEntity>>(emptyList())
    val terminalHistory: StateFlow<List<TerminalHistoryEntity>> = _terminalHistory.asStateFlow()

    // Clipboard History
    private val _clipboardHistory = MutableStateFlow<List<ClipboardEntity>>(emptyList())
    val clipboardHistory: StateFlow<List<ClipboardEntity>> = _clipboardHistory.asStateFlow()

    // File Explorer
    val currentPath = MutableStateFlow(application.getExternalFilesDir(null)?.absolutePath ?: "/sdcard")
    val fileList = MutableStateFlow<List<FileItem>>(emptyList())
    val isFileLoading = MutableStateFlow(false)

    // APK List
    val apkList = MutableStateFlow<List<AppPackageInfo>>(emptyList())

    // AI Chat
    val aiMessages = MutableStateFlow<List<AiChatMessage>>(
        listOf(
            AiChatMessage(
                sender = "ai",
                message = "Hello! I am your Windows 11 Android Desktop Suite Assistant. I can help you debug ADB commands, analyze Logcat output, generate shell scripts, or troubleshoot device performance. How can I assist you today?"
            )
        )
    )
    val isAiThinking = MutableStateFlow(false)

    // Network Ping State
    val pingHost = mutableStateOf("8.8.8.8")
    val pingOutput = mutableStateOf("Click 'Test Ping' to diagnose connection latency.")
    val isPinging = mutableStateOf(false)

    init {
        // Observe Settings
        viewModelScope.launch {
            suiteDao.getSettings().collectLatest { saved ->
                if (saved != null) _settings.value = saved
            }
        }

        // Observe Notes
        viewModelScope.launch {
            suiteDao.getAllNotes().collectLatest { list ->
                _notes.value = list
            }
        }

        // Observe Terminal History
        viewModelScope.launch {
            suiteDao.getTerminalHistory().collectLatest { list ->
                _terminalHistory.value = list
            }
        }

        // Observe Clipboard
        viewModelScope.launch {
            suiteDao.getClipboardHistory().collectLatest { list ->
                _clipboardHistory.value = list
            }
        }

        // Periodic Device Diagnostic Loop
        viewModelScope.launch {
            while (true) {
                _deviceMetrics.value = diagnosticEngine.getDeviceMetrics()
                delay(2000)
            }
        }

        // Load initial file explorer directory
        loadFiles(currentPath.value)

        // Load APK list
        loadInstalledApks()
    }

    // Navigation & Windows Controls
    fun openApp(app: DesktopApp) {
        if (!openWindows.contains(app)) {
            openWindows.add(app)
        }
        activeWindow.value = app
        isStartMenuOpen.value = false
    }

    fun closeApp(app: DesktopApp) {
        openWindows.remove(app)
        if (activeWindow.value == app) {
            activeWindow.value = openWindows.lastOrNull()
        }
    }

    fun toggleStartMenu() {
        isStartMenuOpen.value = !isStartMenuOpen.value
        if (isStartMenuOpen.value) {
            isQuickSettingsOpen.value = false
            isWidgetsOpen.value = false
        }
    }

    fun toggleQuickSettings() {
        isQuickSettingsOpen.value = !isQuickSettingsOpen.value
        if (isQuickSettingsOpen.value) {
            isStartMenuOpen.value = false
            isWidgetsOpen.value = false
        }
    }

    fun toggleWidgets() {
        isWidgetsOpen.value = !isWidgetsOpen.value
        if (isWidgetsOpen.value) {
            isStartMenuOpen.value = false
            isQuickSettingsOpen.value = false
        }
    }

    // Settings
    fun updateSettings(newSettings: DesktopSettingsEntity) {
        _settings.value = newSettings
        viewModelScope.launch(Dispatchers.IO) {
            suiteDao.saveSettings(newSettings)
        }
    }

    // ADB Terminal
    fun runAdbCommand(cmd: String) {
        viewModelScope.launch {
            val (output, isErr) = adbEngine.executeAdbCommand(cmd)
            suiteDao.insertTerminalHistory(
                TerminalHistoryEntity(
                    command = cmd,
                    output = output,
                    isError = isErr
                )
            )
        }
    }

    fun clearTerminal() {
        viewModelScope.launch {
            suiteDao.clearTerminalHistory()
        }
    }

    // File Explorer
    fun loadFiles(path: String) {
        viewModelScope.launch {
            isFileLoading.value = true
            currentPath.value = path
            fileList.value = fileEngine.getFilesForDirectory(path)
            isFileLoading.value = false
        }
    }

    fun zipFile(path: String) {
        viewModelScope.launch {
            val destZip = "$path.zip"
            fileEngine.compressZip(path, destZip)
            loadFiles(currentPath.value)
        }
    }

    fun extractZip(path: String) {
        viewModelScope.launch {
            val destDir = path.removeSuffix(".zip")
            fileEngine.extractZip(path, destDir)
            loadFiles(currentPath.value)
        }
    }

    // APK Manager
    fun loadInstalledApks() {
        viewModelScope.launch(Dispatchers.IO) {
            val sampleApks = listOf(
                AppPackageInfo("Desktop Suite System Core", "com.aistudio.androiddesktopsuite", "2.5.0", 250, 48 * 1024 * 1024L, true, 34),
                AppPackageInfo("Android System WebView", "com.google.android.webview", "122.0.6261.64", 6261064, 92 * 1024 * 1024L, true, 34),
                AppPackageInfo("Google Play Services", "com.google.android.gms", "24.08.12", 240812, 180 * 1024 * 1024L, true, 34),
                AppPackageInfo("Scrcpy Mirror Engine", "com.genymobile.scrcpy", "2.3.1", 231, 14 * 1024 * 1024L, false, 34),
                AppPackageInfo("Terminal ADB Shell", "com.aistudio.term", "1.1.0", 110, 8 * 1024 * 1024L, false, 34),
                AppPackageInfo("7-Zip Compression Utility", "org.7zip.android", "23.01", 2301, 12 * 1024 * 1024L, false, 34),
                AppPackageInfo("Media Player Pro", "com.aistudio.mediaplayer", "3.0.1", 301, 28 * 1024 * 1024L, false, 34)
            )
            apkList.value = sampleApks
        }
    }

    // Notes
    fun addNote(title: String, content: String, colorHex: String) {
        viewModelScope.launch {
            suiteDao.insertNote(
                NoteEntity(
                    title = title.ifBlank { "Untitled Note" },
                    content = content,
                    colorHex = colorHex
                )
            )
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            suiteDao.deleteNote(id)
        }
    }

    // Clipboard
    fun addClipboardText(text: String) {
        if (text.isNotBlank()) {
            viewModelScope.launch {
                suiteDao.insertClipboard(ClipboardEntity(text = text))
            }
        }
    }

    // AI Assistant
    fun sendAiPrompt(promptText: String) {
        if (promptText.isBlank()) return

        val userMsg = AiChatMessage(sender = "user", message = promptText)
        val currentList = aiMessages.value.toMutableList()
        currentList.add(userMsg)
        aiMessages.value = currentList

        viewModelScope.launch {
            isAiThinking.value = true
            val responseText = aiEngine.generateAssistantResponse(promptText)
            val aiMsg = AiChatMessage(sender = "ai", message = responseText)

            val updated = aiMessages.value.toMutableList()
            updated.add(aiMsg)
            aiMessages.value = updated
            isAiThinking.value = false
        }
    }

    // Network Ping
    fun runNetworkPing() {
        viewModelScope.launch {
            isPinging.value = true
            pingOutput.value = "Pinging ${pingHost.value}..."
            val (res, latency) = netEngine.pingAddress(pingHost.value)
            pingOutput.value = res
            isPinging.value = false
        }
    }
}

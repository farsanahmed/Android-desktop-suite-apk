package com.example.data.engine

import android.content.Context
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import dev.mobile.dadb.AdbAuthException
import dev.mobile.dadb.AdbConnectException
import dev.mobile.dadb.AdbConnectionClosedException
import dev.mobile.dadb.AdbException
import dev.mobile.dadb.AdbProtocolException
import dev.mobile.dadb.AdbStreamOpenException
import dev.mobile.dadb.Dadb
import dev.mobile.dadb.DadbDevice
import dev.mobile.dadb.InstallResult
import dev.mobile.dadb.SyncResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetSocketAddress
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.NoSuchAlgorithmException
import java.util.concurrent.TimeUnit

enum class AdbConnectionType {
    USB,
    WIRELESS_DIRECT,
    NETWORK_BRIDGE
}

sealed class AdbConnectionState {
    object Disconnected : AdbConnectionState()
    object Connecting : AdbConnectionState()
    object Pairing : AdbConnectionState()
    data class Connected(val device: AdbDeviceInfo) : AdbConnectionState()
    data class Error(val message: String) : AdbConnectionState()
}

data class AdbDeviceInfo(
    val serial: String,
    val model: String = Build.MODEL ?: "Unknown",
    val manufacturer: String = Build.MANUFACTURER ?: "Unknown",
    val ipAddress: String = "",
    val port: Int = 5555,
    val connectionType: AdbConnectionType = AdbConnectionType.USB,
    val isAuthorized: Boolean = false,
    val batteryPercent: Int = 0,
    val pingLatencyMs: Long = 0
)

class AdbConnectionManager(private val context: Context?) {

    private val _connectionState = MutableStateFlow<AdbConnectionState>(AdbConnectionState.Disconnected)
    val connectionState: StateFlow<AdbConnectionState> = _connectionState.asStateFlow()

    private val _activeDevice = MutableStateFlow<AdbDeviceInfo?>(null)
    val activeDevice: StateFlow<AdbDeviceInfo?> = _activeDevice.asStateFlow()

    private val _connectedDevices = MutableStateFlow<List<AdbDeviceInfo>>(emptyList())
    val connectedDevices: StateFlow<List<AdbDeviceInfo>> = _connectedDevices.asStateFlow()

    private var dadbInstance: Dadb? = null
    private var connectionJob: Job? = null
    private var logcatJob: Job? = null
    private var logcatChannel: Channel<String>? = null
    private var logcatLevel = "V"
    private var logcatSearchQuery = ""
    private var logcatPaused = false

    init {
        // No default connected device - user must explicitly connect
    }

    suspend fun pairWireless(ip: String, port: Int, pairingCode: String): Pair<String, Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val dadb = Dadb.create(ip, port)
            val result = dadb.pair(pairingCode)
            if (result) {
                Pair("Successfully paired with $ip:$port", false)
            } else {
                Pair("Pairing failed - invalid code or device not in pairing mode", true)
            }
        } catch (e: AdbAuthException) {
            Pair("Authentication failed: ${e.message}", true)
        } catch (e: AdbConnectException) {
            Pair("Connection failed: ${e.message}", true)
        } catch (e: Exception) {
            Pair("Pairing error: ${e.message}", true)
        }
    }

    suspend fun connectWireless(ip: String, port: Int): Pair<String, Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            _connectionState.value = AdbConnectionState.Connecting
            val dadb = Dadb.create(ip, port)
            
            // Test connection with a simple shell command
            val testResult = dadb.shell("echo connected")
            if (testResult.exitCode == 0) {
                dadbInstance = dadb
                
                // Get device info
                val propsResult = dadb.shell("getprop ro.product.model; getprop ro.product.manufacturer; getprop ro.serialno; cat /sys/class/power_supply/battery/capacity 2>/dev/null || echo 0")
                val props = propsResult.output.trim().split("\n")
                val model = props.getOrNull(0) ?: Build.MODEL ?: "Unknown"
                val manufacturer = props.getOrNull(1) ?: Build.MANUFACTURER ?: "Unknown"
                val serial = props.getOrNull(2) ?: "unknown"
                val battery = props.getOrNull(3)?.toIntOrNull() ?: 0
                
                val deviceInfo = AdbDeviceInfo(
                    serial = serial,
                    model = model,
                    manufacturer = manufacturer,
                    ipAddress = ip,
                    port = port,
                    connectionType = AdbConnectionType.WIRELESS_DIRECT,
                    isAuthorized = true,
                    batteryPercent = battery
                )
                
                _activeDevice.value = deviceInfo
                _connectedDevices.value = listOf(deviceInfo)
                _connectionState.value = AdbConnectionState.Connected(deviceInfo)
                Pair("Connected to $ip:$port ($model)", false)
            } else {
                _connectionState.value = AdbConnectionState.Error("Device not responding")
                Pair("Device not responding", true)
            }
        } catch (e: AdbAuthException) {
            _connectionState.value = AdbConnectionState.Error("Unauthorized: ${e.message}")
            Pair("Unauthorized - enable USB debugging and authorize this computer", true)
        } catch (e: AdbConnectException) {
            _connectionState.value = AdbConnectionState.Error("Connection failed: ${e.message}")
            Pair("Connection failed: ${e.message}", true)
        } catch (e: Exception) {
            _connectionState.value = AdbConnectionState.Error("Error: ${e.message}")
            Pair("Error: ${e.message}", true)
        }
    }

    suspend fun connectUsb(serial: String? = null): Pair<String, Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            _connectionState.value = AdbConnectionState.Connecting
            
            val dadb = if (serial != null) {
                // Connect via adb server with specific serial
                Dadb.create("localhost", 5037, "host:transport:$serial")
            } else {
                Dadb.discover() ?: return@withContext {
                    _connectionState.value = AdbConnectionState.Error("No USB device found")
                    Pair("No USB device found. Enable USB debugging.", true)
                }
            }
            
            val testResult = dadb.shell("echo connected")
            if (testResult.exitCode == 0) {
                dadbInstance = dadb
                
                val propsResult = dadb.shell("getprop ro.product.model; getprop ro.product.manufacturer; getprop ro.serialno; cat /sys/class/power_supply/battery/capacity 2>/dev/null || echo 0")
                val props = propsResult.output.trim().split("\n")
                val model = props.getOrNull(0) ?: Build.MODEL ?: "Unknown"
                val manufacturer = props.getOrNull(1) ?: Build.MANUFACTURER ?: "Unknown"
                val deviceSerial = props.getOrNull(2) ?: "unknown"
                val battery = props.getOrNull(3)?.toIntOrNull() ?: 0
                
                val deviceInfo = AdbDeviceInfo(
                    serial = deviceSerial,
                    model = model,
                    manufacturer = manufacturer,
                    connectionType = AdbConnectionType.USB,
                    isAuthorized = true,
                    batteryPercent = battery
                )
                
                _activeDevice.value = deviceInfo
                _connectedDevices.value = listOf(deviceInfo)
                _connectionState.value = AdbConnectionState.Connected(deviceInfo)
                Pair("Connected via USB ($model)", false)
            } else {
                _connectionState.value = AdbConnectionState.Error("Device not responding")
                Pair("Device not responding", true)
            }
        } catch (e: AdbAuthException) {
            _connectionState.value = AdbConnectionState.Error("Unauthorized: ${e.message}")
            Pair("Unauthorized - authorize this computer on device", true)
        } catch (e: AdbConnectException) {
            _connectionState.value = AdbConnectionState.Error("Connection failed: ${e.message}")
            Pair("Connection failed: ${e.message}", true)
        } catch (e: Exception) {
            _connectionState.value = AdbConnectionState.Error("Error: ${e.message}")
            Pair("Error: ${e.message}", true)
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        stopLogcat()
        connectionJob?.cancel()
        dadbInstance?.close()
        dadbInstance = null
        _activeDevice.value = null
        _connectedDevices.value = emptyList()
        _connectionState.value = AdbConnectionState.Disconnected
        Pair("Disconnected", false)
    }

    suspend fun executeShellCommand(command: String): Pair<String, Boolean> = withContext(Dispatchers.IO) {
        val dadb = dadbInstance ?: return@withContext Pair("Not connected to any device", true)
        
        return@withContext try {
            val result = dadb.shell(command)
            if (result.exitCode == 0) {
                Pair(result.output.ifEmpty { "Command executed successfully (no output)" }, false)
            } else {
                Pair("Exit code ${result.exitCode}: ${result.output}", true)
            }
        } catch (e: AdbConnectionClosedException) {
            _connectionState.value = AdbConnectionState.Error("Connection closed")
            disconnect()
            Pair("Connection lost: ${e.message}", true)
        } catch (e: AdbException) {
            Pair("ADB error: ${e.message}", true)
        } catch (e: Exception) {
            Pair("Error: ${e.message}", true)
        }
    }

    suspend fun pushFile(localPath: String, remotePath: String): Pair<String, Boolean> = withContext(Dispatchers.IO) {
        val dadb = dadbInstance ?: return@withContext Pair("Not connected", true)
        val file = File(localPath)
        
        return@withContext try {
            if (!file.exists()) {
                Pair("Local file not found: $localPath", true)
            } else {
                val result = dadb.push(file, remotePath)
                when (result) {
                    is SyncResult.Success -> Pair("Pushed ${file.name} to $remotePath (${result.bytesTransferred} bytes)", false)
                    is SyncResult.Failure -> Pair("Push failed: ${result.reason}", true)
                }
            }
        } catch (e: AdbException) {
            Pair("ADB error: ${e.message}", true)
        } catch (e: Exception) {
            Pair("Error: ${e.message}", true)
        }
    }

    suspend fun pullFile(remotePath: String, localPath: String): Pair<String, Boolean> = withContext(Dispatchers.IO) {
        val dadb = dadbInstance ?: return@withContext Pair("Not connected", true)
        
        return@withContext try {
            val localFile = File(localPath)
            localFile.parentFile?.mkdirs()
            val result = dadb.pull(localFile, remotePath)
            when (result) {
                is SyncResult.Success -> Pair("Pulled $remotePath to $localPath (${result.bytesTransferred} bytes)", false)
                is SyncResult.Failure -> Pair("Pull failed: ${result.reason}", true)
            }
        } catch (e: AdbException) {
            Pair("ADB error: ${e.message}", true)
        } catch (e: Exception) {
            Pair("Error: ${e.message}", true)
        }
    }

    suspend fun installApk(apkPath: String): Pair<String, Boolean> = withContext(Dispatchers.IO) {
        val dadb = dadbInstance ?: return@withContext Pair("Not connected", true)
        val file = File(apkPath)
        
        return@withContext try {
            if (!file.exists()) {
                Pair("APK file not found: $apkPath", true)
            } else {
                val result = dadb.install(file)
                when (result) {
                    is InstallResult.Success -> Pair("Successfully installed ${file.name}", false)
                    is InstallResult.Failure -> Pair("Install failed: ${result.reason}", true)
                }
            }
        } catch (e: AdbException) {
            Pair("ADB error: ${e.message}", true)
        } catch (e: Exception) {
            Pair("Error: ${e.message}", true)
        }
    }

    suspend fun uninstallPackage(packageName: String): Pair<String, Boolean> = withContext(Dispatchers.IO) {
        val dadb = dadbInstance ?: return@withContext Pair("Not connected", true)
        
        return@withContext try {
            val result = dadb.uninstall(packageName)
            when (result) {
                is dev.mobile.dadb.UninstallResult.Success -> Pair("Successfully uninstalled $packageName", false)
                is dev.mobile.dadb.UninstallResult.Failure -> Pair("Uninstall failed: ${result.reason}", true)
            }
        } catch (e: AdbException) {
            Pair("ADB error: ${e.message}", true)
        } catch (e: Exception) {
            Pair("Error: ${e.message}", true)
        }
    }

    fun startLogcat(level: String = "V", searchQuery: String = "", onNewLine: (String) -> Unit): Job {
        logcatLevel = level
        logcatSearchQuery = searchQuery
        logcatPaused = false
        logcatChannel = Channel<String>(Channel.UNLIMITED)
        
        return CoroutineScope(Dispatchers.IO).launch {
            val dadb = dadbInstance ?: return@launch
            logcatJob = CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Build logcat command with level filter
                    val levelFilter = when (level) {
                        "V" -> "*:V"
                        "D" -> "*:D"
                        "I" -> "*:I"
                        "W" -> "*:W"
                        "E" -> "*:E"
                        "F" -> "*:F"
                        else -> "*:V"
                    }
                    val cmd = "logcat -v brief $levelFilter"
                    
                    val process = dadb.shell(cmd, true) // streaming shell
                    // Note: dadb's shell doesn't support streaming directly
                    // We'll use a polling approach or run logcat -d for dumps
                    // For real streaming, we'd need a different approach
                    // For now, use logcat -d (dump) periodically
                    while (isActive && !logcatPaused) {
                        val dumpResult = dadb.shell("logcat -d -v brief $levelFilter -t 100")
                        if (dumpResult.exitCode == 0) {
                            val lines = dumpResult.output.lines()
                            for (line in lines) {
                                if (logcatSearchQuery.isBlank() || line.contains(logcatSearchQuery, ignoreCase = true)) {
                                    logcatChannel?.trySend(line)
                                }
                            }
                        }
                        Thread.sleep(1000)
                    }
                } catch (e: Exception) {
                    logcatChannel?.trySend("[Logcat error: ${e.message}]")
                } finally {
                    logcatChannel?.close()
                }
            }
        }
    }

    fun stopLogcat() {
        logcatPaused = true
        logcatJob?.cancel()
        logcatChannel?.close()
        logcatJob = null
        logcatChannel = null
    }

    fun pauseLogcat() {
        logcatPaused = true
    }

    fun resumeLogcat() {
        logcatPaused = false
    }

    fun setLogcatLevel(level: String) {
        logcatLevel = level
    }

    fun setLogcatSearch(query: String) {
        logcatSearchQuery = query
    }

    val logcatStream: ReceiveChannel<String>
        get() = logcatChannel ?: Channel(Channel.UNLIMITED).also { logcatChannel = it }

    suspend fun clearLogcat(): Pair<String, Boolean> = withContext(Dispatchers.IO) {
        val dadb = dadbInstance ?: return@withContext Pair("Not connected", true)
        return@withContext try {
            val result = dadb.shell("logcat -c")
            if (result.exitCode == 0) {
                Pair("Logcat cleared", false)
            } else {
                Pair("Failed to clear logcat", true)
            }
        } catch (e: Exception) {
            Pair("Error: ${e.message}", true)
        }
    }

    suspend fun getDeviceProperties(): Map<String, String> = withContext(Dispatchers.IO) {
        val dadb = dadbInstance ?: return@withContext emptyMap()
        try {
            val result = dadb.shell("getprop")
            if (result.exitCode == 0) {
                val props = mutableMapOf<String, String>()
                result.output.lines().forEach { line ->
                    val parts = line.split("[", limit = 2)
                    if (parts.size == 2) {
                        val key = parts[0].trim()
                        val value = parts[1].removeSuffix("]").trim()
                        props[key] = value
                    }
                }
                props
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    suspend fun isConnected(): Boolean = withContext(Dispatchers.IO) {
        val dadb = dadbInstance ?: return@withContext false
        try {
            val result = dadb.shell("echo test")
            result.exitCode == 0
        } catch (e: Exception) {
            false
        }
    }
}

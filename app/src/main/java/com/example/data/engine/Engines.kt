package com.example.data.engine

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.util.Log
import com.example.data.model.AppPackageInfo
import com.example.data.model.DeviceMetrics
import com.example.data.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.Collections
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class DeviceDiagnosticEngine(private val context: Context) {

    suspend fun getDeviceMetrics(): DeviceMetrics = withContext(Dispatchers.IO) {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        val batteryLevel = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val batteryScale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val pct = if (batteryLevel >= 0 && batteryScale > 0) {
            (batteryLevel / batteryScale.toFloat() * 100).toInt()
        } else {
            -1 // N/A
        }
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val tempTenths = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        val batteryTempC = if (tempTenths > 0) tempTenths / 10.0f else -1f

        // Memory info
        val memInfo = ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(memInfo)
        val ramTotalMb = (memInfo.totalMem / (1024 * 1024)).toLong()
        val ramUsedMb = ramTotalMb - (memInfo.availMem / (1024 * 1024)).toLong()

        // Storage info
        val storagePath = Environment.getDataDirectory()
        val statFs = StatFs(storagePath.path)
        val blockSize = statFs.blockSizeLong
        val totalBlocks = statFs.blockCountLong
        val availableBlocks = statFs.availableBlocksLong
        val storageTotalGb = (totalBlocks * blockSize / (1024.0 * 1024.0 * 1024.0)).toFloat()
        val storageUsedGb = ((totalBlocks - availableBlocks) * blockSize / (1024.0 * 1024.0 * 1024.0)).toFloat()

        // CPU info
        val cpuCores = Runtime.getRuntime().availableProcessors()
        val cpuUsagePercent = getCpuUsage() // -1 if not available

        // Network info
        val ipAddress = getLocalIpAddress() ?: "N/A"
        val macAddress = getMacAddress() ?: "N/A"

        // Root & bootloader status
        val isRooted = checkRoot()
        val isBootloaderUnlocked = checkBootloaderUnlocked()

        // GPU info
        val gpuModel = getGpuModel()

        // Serial number (requires READ_PRIVILEGED_PHONE_STATE on Android 10+)
        val serialNumber = getSerialNumber()

        DeviceMetrics(
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            androidVersion = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            batteryLevel = pct,
            isCharging = isCharging,
            batteryTempC = batteryTempC,
            ramUsedMb = ramUsedMb,
            ramTotalMb = ramTotalMb,
            storageUsedGb = storageUsedGb,
            storageTotalGb = storageTotalGb,
            cpuUsagePercent = cpuUsagePercent,
            cpuCores = cpuCores,
            gpuModel = gpuModel,
            serialNumber = serialNumber,
            ipAddress = ipAddress,
            macAddress = macAddress,
            isRooted = isRooted,
            isBootloaderUnlocked = isBootloaderUnlocked,
            adbAuthorized = false, // Will be set by ADB connection manager
            connectionType = "Unknown"
        )
    }

    private fun getCpuUsage(): Int {
        // Reading /proc/stat for CPU usage - returns -1 if not available
        return try {
            val reader = InputStreamReader(File("/proc/stat").inputStream())
            val firstLine = reader.readText().lines().firstOrNull()
            if (firstLine != null && firstLine.startsWith("cpu ")) {
                val parts = firstLine.split("\\s+".toRegex()).drop(1).map { it.toLong() }
                val total = parts.sum()
                val idle = parts.getOrNull(3) ?: 0
                // This is a simplified calculation - real CPU usage needs two samples
                ((total - idle) * 100 / total).toInt()
            } else {
                -1
            }
        } catch (e: Exception) {
            -1 // N/A - cannot read CPU stats
        }
    }

    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (iface in interfaces) {
                if (iface.isUp && !iface.isLoopback) {
                    val addresses = Collections.list(iface.inetAddresses)
                    for (addr in addresses) {
                        if (addr is java.net.Inet4Address && !addr.isLoopbackAddress) {
                            return addr.hostAddress
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        return null
    }

    private fun getMacAddress(): String? {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (iface in interfaces) {
                if (iface.isUp && !iface.isLoopback) {
                    val hardwareAddr = iface.hardwareAddress
                    if (hardwareAddr != null) {
                        return hardwareAddr.joinToString(":") { "%02X".format(it) }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        return null
    }

    private fun checkRoot(): Boolean {
        val rootPaths = listOf(
            "/system/bin/su", "/system/xbin/su", "/system/sbin/su",
            "/sbin/su", "/vendor/bin/su", "/su/bin/su", "/magisk/.core/bin/su"
        )
        return rootPaths.any { File(it).exists() }
    }

    private fun checkBootloaderUnlocked(): Boolean {
        return try {
            val result = Runtime.getRuntime().exec("getprop ro.bootloader").inputStream().readText().trim()
            result.isNotBlank() && !result.equals("unknown", ignoreCase = true) && !result.equals("locked", ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }

    private fun getGpuModel(): String {
        return try {
            val gpuInfo = Runtime.getRuntime().exec("getprop ro.hardware.gpu").inputStream().readText().trim()
            if (gpuInfo.isNotBlank()) gpuInfo else {
                val glRenderer = Runtime.getRuntime().exec("getprop debug.egl.profiler").inputStream().readText().trim()
                if (glRenderer.isNotBlank()) glRenderer else "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun getSerialNumber(): String {
        return try {
            // On Android 10+, this requires READ_PRIVILEGED_PHONE_STATE
            Build.getSerial()
        } catch (e: Exception) {
            "N/A (requires privileged permission)"
        }
    }

    suspend fun getInstalledPackages(): List<AppPackageInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        packages.map { app ->
            val appInfo = pm.getApplicationInfo(app.packageName, 0)
            val versionInfo = try {
                pm.getPackageInfo(app.packageName, 0)
            } catch (e: Exception) {
                null
            }
            
            val sizeBytes = try {
                val appFile = File(appInfo.sourceDir)
                if (appFile.exists()) appFile.length() else 0L
            } catch (e: Exception) {
                0L
            }
            
            AppPackageInfo(
                appName = pm.getApplicationLabel(appInfo).toString(),
                packageName = app.packageName,
                versionName = versionInfo?.versionName ?: "Unknown",
                versionCode = versionInfo?.longVersionCode ?: 0,
                sizeBytes = sizeBytes,
                isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                targetSdk = appInfo.targetSdkVersion,
                isInstalled = true
            )
        }.toList()
    }
}

class AdbFastbootEngine(private val connectionManager: AdbConnectionManager) {

    suspend fun executeAdbCommand(command: String): Pair<String, Boolean> = withContext(Dispatchers.IO) {
        // Strip "adb " prefix if present
        val cleanCmd = command.trim().removePrefix("adb ").trim()
        return connectionManager.executeShellCommand(cleanCmd)
    }

    // Fastboot commands are NOT available over network ADB
    // Fastboot requires USB connection in bootloader mode
    suspend fun executeFastbootCommand(command: String): Pair<String, Boolean> = withContext(Dispatchers.IO) {
        return Pair(
            "Fastboot is not available over network ADB.\n" +
            "Fastboot requires:\n" +
            "1. Device in bootloader/fastboot mode (power + volume down)\n" +
            "2. USB connection to host computer\n" +
            "3. Host running fastboot binary\n\n" +
            "This app cannot execute fastboot commands remotely.\n" +
            "Use a computer with Android SDK Platform Tools instead.",
            true
        )
    }
}

class FileCompressionEngine {

    private val MAX_EXTRACT_SIZE = 1L * 1024 * 1024 * 1024 // 1GB limit
    private val MAX_FILE_COUNT = 10000

    suspend fun getFilesForDirectory(path: String): List<FileItem> = withContext(Dispatchers.IO) {
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) {
            return@withContext emptyList()
        }
        
        return@withContext try {
            dir.listFiles()?.map { file ->
                FileItem(
                    name = file.name,
                    path = file.absolutePath,
                    sizeBytes = if (file.isDirectory) 0 else file.length(),
                    isDirectory = file.isDirectory,
                    lastModified = file.lastModified(),
                    permissions = getPermissions(file)
                )
            }.sortedBy { it.name }.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getPermissions(file: File): String {
        val sb = StringBuilder()
        sb.append(if (file.canRead()) "r" else "-")
        sb.append(if (file.canWrite()) "w" else "-")
        sb.append(if (file.canExecute()) "x" else "-")
        sb.append(if (file.canRead()) "r" else "-")
        sb.append(if (file.canWrite()) "w" else "-")
        sb.append(if (file.canExecute()) "x" else "-")
        sb.append(if (file.canRead()) "r" else "-")
        sb.append(if (file.canWrite()) "w" else "-")
        sb.append(if (file.canExecute()) "x" else "-")
        return sb.toString()
    }

    suspend fun compressZip(sourcePath: String, zipPath: String): String = withContext(Dispatchers.IO) {
        try {
            val zipFile = File(zipPath)
            zipFile.parentFile?.mkdirs()
            
            var fileCount = 0
            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                val sourceFile = File(sourcePath)
                if (sourceFile.isDirectory) {
                    addToZip(sourceFile, sourceFile.name, zos)
                } else {
                    addToZip(sourceFile, sourceFile.name, zos)
                }
            }
            "Successfully compressed to $zipPath"
        } catch (e: Exception) {
            "Compression failed: ${e.message}"
        }
    }

    private fun addToZip(file: File, path: String, zos: ZipOutputStream) {
        if (file.isDirectory) {
            zos.putNextEntry(ZipEntry("$path/"))
            zos.closeEntry()
            file.listFiles()?.forEach { child ->
                addToZip(child, "$path/${child.name}", zos)
            }
        } else {
            zos.putNextEntry(ZipEntry(path))
            FileInputStream(file).use { fis ->
                fis.copyTo(zos)
            }
            zos.closeEntry()
        }
    }

    suspend fun extractZip(zipPath: String, destDirPath: String, progressCallback: ((Int, Int) -> Unit)? = null): String = withContext(Dispatchers.IO) {
        try {
            val zipFile = File(zipPath)
            val destDir = File(destDirPath)
            
            if (!zipFile.exists()) {
                return "Error: ZIP file not found"
            }
            if (!destDir.exists()) {
                destDir.mkdirs()
            }
            
            // Canonical destination path for Zip Slip protection
            val canonicalDest = destDir.canonicalPath
            
            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                var fileCount = 0
                var totalSize = 0L
                
                while (entry != null) {
                    if (fileCount >= MAX_FILE_COUNT) {
                        return@withContext "Error: Maximum file count ($MAX_FILE_COUNT) exceeded"
                    }
                    
                    // Zip Slip protection: canonical path check
                    val newFile = File(destDir, entry.name).canonicalFile
                    if (!newFile.path.startsWith(canonicalDest)) {
                        return@withContext "Error: Zip Slip attempt detected - entry: ${entry.name}"
                    }
                    
                    // Check total extracted size
                    if (entry.size > 0 && totalSize + entry.size > MAX_EXTRACT_SIZE) {
                        return@withContext "Error: Extracted size would exceed ${MAX_EXTRACT_SIZE / (1024*1024*1024)}GB limit"
                    }
                    
                    if (entry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()
                        FileOutputStream(newFile).use { fos ->
                            zis.copyTo(fos)
                        }
                        totalSize += entry.size
                    }
                    
                    fileCount++
                    progressCallback?.invoke(fileCount, MAX_FILE_COUNT)
                    entry = zis.nextEntry
                }
                
                "Successfully extracted $fileCount files (${formatSize(totalSize)}) to $destDirPath"
            }
        } catch (e: Exception) {
            "Extraction failed: ${e.message}"
        }
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1L * 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
            bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format("%.2f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}

class NetworkDiagnosticsEngine {

    suspend fun pingAddress(host: String): Pair<String, Long> = withContext(Dispatchers.IO) {
        val startTime = SystemClock.elapsedRealtime()
        return@withContext try {
            // Try ICMP ping first (requires root for raw sockets)
            // Fall back to TCP connection to port 80/443
            val address = InetAddress.getByName(host)
            val reachable = address.isReachable(3000)
            val latency = SystemClock.elapsedRealtime() - startTime
            
            if (reachable) {
                Pair("Reply from ${address.hostAddress}: time=${latency}ms", latency)
            } else {
                // Try TCP connection as fallback
                val tcpLatency = tryTcpConnection(host)
                if (tcpLatency >= 0) {
                    Pair("TCP connection to $host succeeded: time=${tcpLatency}ms", tcpLatency)
                } else {
                    Pair("Ping request to $host timed out.", -1)
                }
            }
        } catch (e: Exception) {
            Pair("Ping error: ${e.message}", -1)
        }
    }

    private fun tryTcpConnection(host: String): Long {
        val ports = intArrayOf(80, 443, 5555, 22)
        for (port in ports) {
            val start = SystemClock.elapsedRealtime()
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(host, port), 2000)
                socket.close()
                return SystemClock.elapsedRealtime() - start
            } catch (e: Exception) {
                // Try next port
            }
        }
        return -1
    }

    suspend fun getNetworkInfo(): String = withContext(Dispatchers.IO) {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return "No active network"
            val caps = cm.getNetworkCapabilities(network) ?: return "No network capabilities"
            
            val sb = StringBuilder()
            sb.appendLine("Network Type: ${when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> "Other"
            }}")
            sb.appendLine("Internet: ${caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)}")
            sb.appendLine("Validated: ${caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)}")
            sb.appendLine("Metered: ${caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)}")
            
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                val linkUp = caps.linkUpBandwidthKbps
                val linkDown = caps.linkDownBandwidthKbps
                sb.appendLine("Link Up: ${linkUp} Kbps")
                sb.appendLine("Link Down: ${linkDown} Kbps")
            }
            
            sb.toString()
        } catch (e: Exception) {
            "Error getting network info: ${e.message}"
        }
    }
}

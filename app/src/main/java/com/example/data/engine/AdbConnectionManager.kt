package com.example.data.engine

import android.content.Context
import android.os.Build
import dadb.AdbAuthException
import dadb.AdbConnectException
import dadb.AdbConnectionClosedException
import dadb.AdbException
import dadb.AdbProtocolException
import dadb.AdbStreamOpenException
import dadb.Dadb
import dadb.DadbDevice
import dadb.InstallResult
import dadb.SyncResult
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
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * ADB Connection Manager - handles ADB connections via DADB library
 */
class AdbConnectionManager(private val context: Context?) {

    enum class AdbConnectionType {
        USB, TCP, WIRELESS
    }

    sealed class AdbConnectionState {
        data class Connecting(val type: AdbConnectionType) : AdbConnectionState()
        data class Connected(val device: AdbDeviceInfo) : AdbConnectionState()
        data class Error(val message: String) : AdbConnectionState()
        object Disconnected : AdbConnectionState()
    }

    data class AdbDeviceInfo(
        val serial: String,
        val model: String,
        val product: String,
        val type: AdbConnectionType
    )

    private val _state = MutableStateFlow<AdbConnectionState>(AdbConnectionState.Disconnected)
    val state: StateFlow<AdbConnectionState> = _state.asStateFlow()

    private var dadb: Dadb? = null
    private var device: DadbDevice? = null
    private var scope: CoroutineScope? = null
    private var keyPair: KeyPair? = null

    init {
        generateKeyPair()
    }

    private fun generateKeyPair() {
        try {
            val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
            keyPairGenerator.initialize(2048)
            keyPair = keyPairGenerator.generateKeyPair()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
    }

    fun connect(type: AdbConnectionType, address: String? = null, port: Int = 5555) {
        _state.value = AdbConnectionState.Connecting(type)
        
        scope = CoroutineScope(Dispatchers.IO + Job())
        scope?.launch {
            try {
                when (type) {
                    AdbConnectionType.USB -> connectUsb()
                    AdbConnectionType.TCP -> connectTcp(address!!, port)
                    AdbConnectionType.WIRELESS -> connectWireless(address!!, port)
                }
            } catch (e: Exception) {
                _state.value = AdbConnectionState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun connectUsb() {
        // USB ADB requires special permissions not available in standard app context
        // This is a placeholder - actual USB ADB would need root or special permissions
        _state.value = AdbConnectionState.Error("USB ADB not available in standard app context")
    }

    private fun connectTcp(address: String, port: Int) {
        try {
            dadb = Dadb()
            device = dadb!!.connect(InetSocketAddress(address, port), keyPair!!)
            val info = AdbDeviceInfo(
                serial = device!!.serial ?: "unknown",
                model = device!!.model ?: "unknown",
                product = device!!.product ?: "unknown",
                type = AdbConnectionType.TCP
            )
            _state.value = AdbConnectionState.Connected(info)
        } catch (e: Exception) {
            _state.value = AdbConnectionState.Error(e.message ?: "TCP connection failed")
        }
    }

    private fun connectWireless(address: String, port: Int) {
        connectTcp(address, port)
    }

    fun disconnect() {
        scope?.coroutineContext.cancelChildren()
        scope = null
        try {
            device?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        device = null
        dadb = null
        _state.value = AdbConnectionState.Disconnected
    }

    suspend fun executeShell(command: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val response = device?.shell(command)
                response?.output?.decodeToString() ?: "No device connected"
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        }
    }

    suspend fun installApk(file: File): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val result = device?.install(file.absolutePath)
                result == InstallResult.Success
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun pushFile(local: File, remote: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val result = device?.sync().push(local.absolutePath, remote)
                result == SyncResult.Success
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun pullFile(remote: String, local: File): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val result = device?.sync().pull(remote, local.absolutePath)
                result == SyncResult.Success
            } catch (e: Exception) {
                false
            }
        }
    }

    fun generateKeyPairForWireless(): KeyPair? {
        return keyPair
    }

    companion object {
        fun generateSecureRandom(): ByteArray {
            val bytes = ByteArray(32)
            try {
                val keyGenerator = KeyGenerator.getInstance("AES")
                keyGenerator.init(256)
                val secretKey = keyGenerator.generateKey()
                return secretKey.encoded
            } catch (e: NoSuchAlgorithmException) {
                // Fallback to SecureRandom
                java.security.SecureRandom().nextBytes(bytes)
                return bytes
            }
        }
    }
}

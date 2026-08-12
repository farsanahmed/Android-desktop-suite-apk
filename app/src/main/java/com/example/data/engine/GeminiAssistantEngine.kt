package com.example.data.engine

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiAssistantEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun generateAssistantResponse(prompt: String, conversationContext: String = ""): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getOfflineAdbKnowledgeFallback(prompt)
        }

        val systemPrompt = """
            You are the Windows 11 Android Desktop Suite AI Assistant.
            You specialize in Android device management, ADB & Fastboot commands, Logcat troubleshooting, system performance tuning, and APK management.
            Provide clear, concise, actionable responses with code or shell blocks when requested.
        """.trimIndent()

        val fullPrompt = if (conversationContext.isNotBlank()) {
            "Context:\n$conversationContext\n\nUser Question:\n$prompt"
        } else {
            prompt
        }

        try {
            // Build Request JSON via org.json
            val jsonRoot = JSONObject()

            // System Instruction
            val sysContent = JSONObject()
            val sysParts = JSONArray()
            sysParts.put(JSONObject().put("text", systemPrompt))
            sysContent.put("parts", sysParts)
            jsonRoot.put("systemInstruction", sysContent)

            // User Contents
            val contentsArray = JSONArray()
            val userContent = JSONObject()
            val userParts = JSONArray()
            userParts.put(JSONObject().put("text", fullPrompt))
            userContent.put("parts", userParts)
            contentsArray.put(userContent)
            jsonRoot.put("contents", contentsArray)

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(jsonRoot.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful && bodyStr.isNotBlank()) {
                    val resJson = JSONObject(bodyStr)
                    val candidates = resJson.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val candidate = candidates.getJSONObject(0)
                        val content = candidate.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text", getOfflineAdbKnowledgeFallback(prompt))
                        }
                    }
                }
                getOfflineAdbKnowledgeFallback(prompt)
            }
        } catch (e: Exception) {
            getOfflineAdbKnowledgeFallback(prompt)
        }
    }

    private fun getOfflineAdbKnowledgeFallback(query: String): String {
        val q = query.lowercase()
        return when {
            q.contains("adb error") || q.contains("unauthorized") -> """
                ### ADB Troubleshooting Guide: Device Unauthorized
                1. **Check USB Debugging Prompt**: Unlock your device screen and accept the RSA key fingerprint dialog.
                2. **Revoke Authorizations**: Run `adb revoke-usb-debugging-authorizations` and reconnect the USB cable.
                3. **Restart ADB Daemon**: Run `adb kill-server` followed by `adb start-server`.
                4. **Wireless ADB**: Ensure both PC/Phone are on the same Wi-Fi subnet, then pair via `adb pair 192.168.x.x:PORT`.
            """.trimIndent()

            q.contains("fastboot") || q.contains("bootloader") -> """
                ### Fastboot Command Quick Reference
                - **Check Connected Devices**: `fastboot devices`
                - **Unlock Bootloader**: `fastboot flashing unlock` (erases data)
                - **Flash Partition**: `fastboot flash boot boot.img` or `fastboot flash system system.img`
                - **Reboot System**: `fastboot reboot`
                - **Reboot Recovery**: `fastboot reboot recovery`
                *Warning: Flashing incorrect images can soft-brick your device. Always verify checksums first.*
            """.trimIndent()

            q.contains("logcat") || q.contains("crash") -> """
                ### Logcat Error Analysis Tip
                - To filter crashes and exceptions only:
                  `adb logcat *:E | grep -i "exception"`
                - To filter logs by tag:
                  `adb logcat -s "AndroidDesktopSuite"`
                - Clear previous logs before capturing:
                  `adb logcat -c`
            """.trimIndent()

            q.contains("apk") || q.contains("install") -> """
                ### Package Manager ADB Commands
                - Install APK: `adb install -r app.apk`
                - Install Multiple Split APKs: `adb install-multiple app1.apk app2.apk`
                - List Third-Party Packages: `adb shell pm list packages -3`
                - Extract APK Path: `adb shell pm path com.example.package`
            """.trimIndent()

            else -> """
                ### Android Desktop Suite AI Assistant
                I am ready to help you with:
                - Explaining ADB and Fastboot error messages
                - Generating custom ADB shell scripts
                - Analyzing Logcat trace dumps and crash reports
                - Guiding device root and bootloader configuration
                - Advising on APK extraction, compression, and file system management

                *Try asking: "How do I fix ADB unauthorized error?" or "Show me fastboot flash commands"*
            """.trimIndent()
        }
    }
}

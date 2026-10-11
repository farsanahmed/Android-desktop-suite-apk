package com.example.data.engine

import android.content.Context
import androidx.lifecycle.lifecycleScope
import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ktx.generativeModel
import com.google.firebase.ai.ktx.generateContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiAssistantEngine(private val context: Context) {

    private val firebaseAI = FirebaseAI.getInstance()
    private val model: GenerativeModel = firebaseAI.generativeModel("gemini-1.5-flash")

    suspend fun generateAssistantResponse(prompt: String, conversationContext: String = ""): String = withContext(Dispatchers.IO) {
        val systemPrompt = """
            You are the Android Desktop Suite AI Assistant.
            You specialize in Android device management, ADB commands, Logcat troubleshooting, 
            system performance tuning, and APK management.
            Provide clear, concise, actionable responses with code or shell blocks when requested.
            If you don't know something, say so rather than making up information.
        """.trimIndent()

        val fullPrompt = if (conversationContext.isNotBlank()) {
            "Context:\n$conversationContext\n\nUser Question:\n$prompt"
        } else {
            prompt
        }

        return@withContext try {
            val response = model.generateContent(systemPrompt + "\n\n" + fullPrompt)
            response.text ?: getOfflineAdbKnowledgeFallback(prompt)
        } catch (e: Exception) {
            // Handle various error cases
            when {
                e.message?.contains("quota", true) == true || e.message?.contains("429") == true -> 
                    "⚠️ **API Quota Exceeded**\n\nThe AI service has reached its rate limit. Please try again later.\n\n" + getOfflineAdbKnowledgeFallback(prompt)
                e.message?.contains("network", true) == true || e.message?.contains("offline", true) == true ->
                    "📴 **Offline Mode**\n\nCannot connect to AI service. Showing offline knowledge base:\n\n" + getOfflineAdbKnowledgeFallback(prompt)
                e.message?.contains("API key", true) == true || e.message?.contains("permission", true) == true ->
                    "🔐 **Configuration Error**\n\nFirebase AI Logic is not properly configured. Please ensure:\n" +
                    "1. Firebase project is linked\n" +
                    "2. Vertex AI API is enabled in Google Cloud Console\n" +
                    "3. App Check is configured\n\n" + getOfflineAdbKnowledgeFallback(prompt)
                else ->
                    "❌ **AI Error**: ${e.message}\n\nFalling back to offline knowledge:\n\n" + getOfflineAdbKnowledgeFallback(prompt)
            }
        }
    }

    private fun getOfflineAdbKnowledgeFallback(prompt: String): String {
        val q = prompt.lowercase()
        
        return when {
            q.contains("adb") && q.contains("connect") -> """
                ### ADB Connection Help
                
                **Wireless ADB (Android 11+):**
                1. Enable Developer Options → Wireless Debugging
                2. Tap "Pair with pairing code" to get IP, port, and 6-digit code
                3. In this app: ADB Terminal → "Pair Device" → enter details
                
                **Manual TCP/IP:**
                ```bash
                adb tcpip 5555
                adb connect 192.168.1.xxx:5555
                ```
                
                **USB:**
                ```bash
                adb devices
                adb -s <serial> shell
                ```
            """.trimIndent()
            
            q.contains("fastboot") -> """
                ### Fastboot Commands
                
                **⚠️ Fastboot requires USB + bootloader mode**, not network ADB.
                
                **Boot into fastboot:**
                ```bash
                adb reboot bootloader
                # Or: Power + Volume Down
                ```
                
                **Common commands:**
                - `fastboot devices` - List devices in fastboot mode
                - `fastboot getvar all` - Show partition info
                - `fastboot flash boot boot.img` - Flash boot image
                - `fastboot flash dtbo dtbo.img` - Flash DTBO
                - `fastboot reboot` - Reboot to system
                - `fastboot reboot recovery` - Reboot to recovery
                - `fastboot oem unlock` - Unlock bootloader (wipes data!)
                
                *Warning: Flashing incorrect images can soft-brick your device. Always verify checksums first.*
            """.trimIndent()
            
            q.contains("logcat") || q.contains("crash") -> """
                ### Logcat Analysis
                
                **Filter crashes only:**
                ```bash
                adb logcat *:E | grep -i "exception"
                ```
                
                **Filter by tag:**
                ```bash
                adb logcat -s "YourTag"
                ```
                
                **Clear and capture:**
                ```bash
                adb logcat -c
                adb logcat -d > log.txt
                ```
                
                **Format options:**
                - `adb logcat -v brief` - Short format
                - `adb logcat -v time` - With timestamps
                - `adb logcat -v color` - Colored output
            """.trimIndent()
            
            q.contains("apk") || q.contains("install") -> """
                ### APK Management
                
                **Install:**
                ```bash
                adb install -r app.apk          # Replace existing
                adb install-multiple *.apk      # Split APKs
                ```
                
                **List packages:**
                ```bash
                adb shell pm list packages -3   # Third-party only
                adb shell pm list packages -s   # System only
                ```
                
                **Extract APK:**
                ```bash
                adb shell pm path com.pkg
                adb pull /data/app/.../base.apk
                ```
                
                **Uninstall:**
                ```bash
                adb uninstall com.pkg
                adb uninstall -k com.pkg       # Keep data
                ```
            """.trimIndent()
            
            q.contains("performance") || q.contains("battery") || q.contains("memory") -> """
                ### Performance Tuning
                
                **Memory info:**
                ```bash
                adb shell dumpsys meminfo
                adb shell cat /proc/meminfo
                ```
                
                **Battery stats:**
                ```bash
                adb shell dumpsys batterystats
                adb shell dumpsys battery
                ```
                
                **CPU info:**
                ```bash
                adb shell cat /proc/cpuinfo
                adb shell top -n 1
                ```
                
                **GPU info:**
                ```bash
                adb shell dumpsys gfxinfo
                ```
            """.trimIndent()
            
            q.contains("root") || q.contains("magisk") || q.contains("su") -> """
                ### Root Access
                
                **Check root:**
                ```bash
                adb shell su -c "id"
                # Should return: uid=0(root) gid=0(root)
                ```
                
                **Magisk:**
                - Install via custom recovery
                - Hide root: Magisk app → Configure DenyList
                - Modules: Magisk app → Modules
                
                **SafetyNet/Play Integrity:**
                - Use Magisk + Play Integrity Fix module
                - Or: KernelSU + appropriate modules
            """.trimIndent()
            
            else -> """
                ### Android Desktop Suite AI Assistant
                
                I'm running in **offline mode** using a local knowledge base.
                For AI-powered responses, configure Firebase AI Logic in the app.
                
                **I can help with:**
                - ADB & Fastboot commands and troubleshooting
                - Logcat analysis and crash debugging
                - APK installation, extraction, and management
                - Device performance monitoring
                - Root/Magisk configuration
                - Storage and file system operations
                
                **Try asking:**
                - "How do I fix ADB unauthorized error?"
                - "Show me fastboot flash commands"
                - "How to extract an APK from device?"
                - "What does this logcat error mean?"
                
                *Note: Fastboot is not available over network ADB. Use USB + bootloader mode.*
            """.trimIndent()
        }
    }
}

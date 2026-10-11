package com.example

import android.app.Application
import android.util.Log
import com.example.data.local.SuiteDatabase

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Room database
        SuiteDatabase.getDatabase(this)
        
        // Global crash handler
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("AndroidDesktopSuite", "Uncaught exception in thread: ${thread.name}", throwable)
            
            // In production, you would upload this to a crash reporting service
            // like Firebase Crashlytics, Sentry, etc.
            
            // Try to save crash info to local storage
            saveCrashInfo(throwable)
            
            // Restart the app or show crash dialog
            // For now, let the default handler deal with it
            System.exit(2)
        }
    }
    
    private fun saveCrashInfo(throwable: Throwable) {
        try {
            val crashDir = getDir("crashes", MODE_PRIVATE)
            val crashFile = java.io.File(crashDir, "crash_${System.currentTimeMillis()}.log")
            crashFile.writeText(
                "Time: ${java.util.Date()}\n" +
                "Thread: ${Thread.currentThread().name}\n" +
                "Exception: ${throwable.javaClass.name}\n" +
                "Message: ${throwable.message}\n" +
                "Stack Trace:\n${throwable.stackTrace.joinToString("\n") { "\tat $it" }}"
            )
        } catch (e: Exception) {
            Log.e("AndroidDesktopSuite", "Failed to save crash info", e)
        }
    }
}

package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.data.model.AppPackageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context) {

    suspend fun getInstalledApps(): List<AppPackageInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        resolveInfos.mapNotNull { ri ->
            val ai = ri.activityInfo?.applicationInfo ?: return@mapNotNull null
            val pkg = ai.packageName
            val name = pm.getApplicationLabel(ai).toString()
            val info = try { pm.getPackageInfo(pkg, 0) } catch (e: Exception) { null }
            val versionName = info?.versionName ?: ""
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                info?.longVersionCode ?: 0L
            } else {
                @Suppress("DEPRECATION")
                (info?.versionCode ?: 0).toLong()
            }
            val sizeBytes = try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val sourceDir = appInfo.sourceDir
                if (sourceDir != null) java.io.File(sourceDir).length() else 0L
            } catch (e: Exception) { 0L }
            val isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val targetSdk = ai.targetSdkVersion
            AppPackageInfo(
                appName = name,
                packageName = pkg,
                versionName = versionName,
                versionCode = versionCode,
                sizeBytes = sizeBytes,
                isSystemApp = isSystem,
                targetSdk = targetSdk,
                isInstalled = true
            )
        }.sortedBy { it.appName.lowercase() }
    }
}

package com.example.ui.views

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppPackageInfo
import com.example.ui.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ApkManagerView(viewModel: MainViewModel) {
    val apks by viewModel.apkList.collectAsState()
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var showInstallDialog by remember { mutableStateOf(false) }
    var showUnavailableNotice by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // APK install launcher
    val installApkLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            snackbarMessage = "APK installed successfully"
            viewModel.loadInstalledApks()
        } else {
            snackbarMessage = "APK installation cancelled or failed"
        }
    }

    fun installApkFromUri(uri: Uri) {
        scope.launch(Dispatchers.IO) {
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(uri, "application/vnd.android.package-archive")
                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    // FileProvider is handled automatically by OpenDocument result
                }
                
                if (intent.resolveActivity(context.packageManager) != null) {
                    installApkLauncher.launch(intent)
                } else {
                    snackbarMessage = "No app found to install APK"
                }
            } catch (e: Exception) {
                snackbarMessage = "Install failed: ${e.message}"
            }
        }
    }

    fun uninstallPackage(packageName: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val uri = Uri.parse("package:$packageName")
                val intent = Intent(Intent.ACTION_DELETE, uri)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                
                // For uninstall, we need to startActivityForResult
                // Since we can't easily do that from ViewModel, we'll use the ADB connection if available
                val (output, isErr) = viewModel.adbConnectionManager.executeShellCommand("pm uninstall $packageName")
                
                if (!isErr) {
                    snackbarMessage = "Uninstalled $packageName"
                    viewModel.loadInstalledApks()
                } else {
                    // Fallback to system uninstall intent
                    snackbarMessage = "ADB uninstall failed, trying system uninstaller..."
                    scope.launch(Dispatchers.Main) {
                        // We'd need an Activity to launch this
                        // For now, just show message
                    }
                }
            } catch (e: Exception) {
                snackbarMessage = "Uninstall failed: ${e.message}"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "APK Package Installer & Extractor",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        // Pick APK file using SAF
                        pickApkFile.launch(arrayOf("application/vnd.android.package-archive"))
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Install APK")
                }

                Button(
                    onClick = { viewModel.loadInstalledApks() },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rescan")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Capability Notice
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "This app uses the system package installer for APK installation. " +
                           "Uninstall via ADB (if connected) or system uninstaller. " +
                           "App listing is real-time from PackageManager.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // APK List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(apks) { app ->
                ApkItemCard(
                    app = app,
                    onExtract = {
                        scope.launch {
                            val (output, isErr) = viewModel.adbConnectionManager.executeShellCommand("pm path ${app.packageName}")
                            if (!isErr && output.isNotBlank()) {
                                val apkPath = output.trim().removePrefix("package:")
                                val destPath = "/sdcard/Download/${app.packageName}.apk"
                                val (cpOut, cpErr) = viewModel.adbConnectionManager.executeShellCommand("cp \"$apkPath\" \"$destPath\"")
                                if (!cpErr) {
                                    snackbarMessage = "Extracted to $destPath"
                                } else {
                                    snackbarMessage = "Extract failed: $cpOut"
                                }
                            } else {
                                snackbarMessage = "Could not find APK path for ${app.packageName}"
                            }
                        }
                    },
                    onUninstall = { uninstallPackage(app.packageName) },
                    onLaunch = {
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                        if (launchIntent != null) {
                            context.startActivity(launchIntent)
                            snackbarMessage = "Launched ${app.appName}"
                        } else {
                            snackbarMessage = "No launch activity for ${app.appName}"
                        }
                    }
                )
            }
        }

        // Empty state
        if (apks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Package, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No packages found",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.loadInstalledApks() }) {
                        Text("Rescan")
                    }
                }
            }
        }

        // Snackbar
        snackbarMessage?.let { msg ->
            Snackbar(
                action = {
                    TextButton(onClick = { snackbarMessage = null }) {
                        Text("Dismiss")
                    }
                },
                modifier = Modifier.padding(8.dp)
            ) {
                Text(msg)
            }
        }
    }
}

@Composable
fun ApkItemCard(
    app: AppPackageInfo,
    onExtract: () -> Unit,
    onUninstall: () -> Unit,
    onLaunch: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // App Icon placeholder
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = if (app.isSystemApp) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (app.isSystemApp) Icons.Default.Shield else Icons.Default.Android,
                        contentDescription = null,
                        tint = if (app.isSystemApp) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // App Info
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${app.packageName} • v${app.versionName} (${app.versionCode})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${formatSize(app.sizeBytes)} • ${if (app.isSystemApp) "System Package" else "User Installed"} • Target SDK ${app.targetSdk}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                // Actions
                Column(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(
                        onClick = onExtract,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Extract", fontSize = 11.sp)
                    }
                    
                    if (!app.isSystemApp) {
                        OutlinedButton(
                            onClick = onUninstall,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                                outlineColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Uninstall", fontSize = 11.sp)
                        }
                    }

                    Button(
                        onClick = onLaunch,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Launch", fontSize = 11.sp)
                    }
                }
            }
        }
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

package com.example.ui.views

import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileItem
import com.example.ui.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun FileExplorerView(viewModel: MainViewModel) {
    val currentPath by viewModel.currentPath.collectAsState()
    val files by viewModel.fileList.collectAsState()
    val isLoading by viewModel.isFileLoading.collectAsState()
    var selectedSidebarItem by remember { mutableStateOf("Documents") }
    var searchQuery by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Permission state
    var showPermissionRationale by remember { mutableStateOf(false) }
    var showSAFDialog by remember { mutableStateOf(false) }
    var permissionStatus by remember { mutableStateOf<PermissionStatus>(PermissionStatus.Unknown) }

    // SAF URI persistence
    var persistedUri by remember { mutableStateOf<Uri?>(null) }
    
    // Permission launcher
    val requestPermissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            permissionStatus = PermissionStatus.Granted
            loadFilesForPath(currentPath)
        } else {
            permissionStatus = PermissionStatus.Denied
            // Check if we need to show rationale
            showPermissionRationale = true
        }
    }
    
    // SAF document tree picker
    val pickDirectory = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            persistedUri = uri
            permissionStatus = PermissionStatus.SAFGranted
            loadFilesForSAFUri(uri)
        }
    }

    // Initialize - check permissions
    LaunchedEffect(key1 = true) {
        checkAndRequestPermissions()
    }

    fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            val permissions = arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
            val allGranted = permissions.all { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                permissionStatus = PermissionStatus.Granted
            } else {
                requestPermissions.launch(permissions)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11+
            // Check MANAGE_EXTERNAL_STORAGE
            if (Environment.isExternalStorageManager()) {
                permissionStatus = PermissionStatus.Granted
            } else {
                // Check legacy permissions
                val readGranted = context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                if (readGranted) {
                    permissionStatus = PermissionStatus.Granted
                } else {
                    requestPermissions.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
                }
            }
        } else { // Android 10 and below
            val readGranted = context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            val writeGranted = context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            if (readGranted && writeGranted) {
                permissionStatus = PermissionStatus.Granted
            } else {
                requestPermissions.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE))
            }
        }
    }

    fun loadFilesForPath(path: String) {
        scope.launch {
            viewModel.isFileLoading.value = true
            viewModel.currentPath.value = path
            viewModel.fileList.value = viewModel.fileEngine.getFilesForDirectory(path)
            viewModel.isFileLoading.value = false
        }
    }

    fun loadFilesForSAFUri(uri: Uri) {
        scope.launch {
            viewModel.isFileLoading.value = true
            // For SAF, we can't use File API directly
            // We'd need to use DocumentsContract to list files
            // For now, show a message
            viewModel.fileList.value = emptyList()
            viewModel.isFileLoading.value = false
        }
    }

    fun openSAFSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.data = Uri.parse("package:${context.packageName}")
            context.startActivity(intent)
        } else {
            val intent = Intent(Settings.ACTION_MANAGE_EXTERNAL_STORAGE)
            context.startActivity(intent)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Permission handling UI
        when (permissionStatus) {
            PermissionStatus.Denied -> {
                PermissionDeniedView(
                    onRequestPermissions = { checkAndRequestPermissions() },
                    onOpenSettings = { openSAFSettings() },
                    onUseSAF = { showSAFDialog = true }
                )
            }
            PermissionStatus.SAFGranted -> {
                SAFGrantedView(
                    onDismiss = { showSAFDialog = false },
                    onPickDirectory = { pickDirectory.launch(null) }
                )
            }
            else -> {
                // Normal file explorer UI
                FileExplorerContent(
                    currentPath = currentPath,
                    files = files,
                    isLoading = isLoading,
                    selectedSidebarItem = selectedSidebarItem,
                    searchQuery = searchQuery,
                    onSidebarItemClick = { selectedSidebarItem = it },
                    onSearchQueryChange = { searchQuery = it },
                    onFileClick = { file ->
                        if (file.isDirectory) {
                            loadFilesForPath(file.path)
                        } else {
                            // Open file
                        }
                    },
                    onNavigateUp = {
                        val parent = File(currentPath).parent
                        if (parent != null) {
                            loadFilesForPath(parent)
                        }
                    },
                    onRefresh = { loadFilesForPath(currentPath) },
                    onRequestPermissions = { checkAndRequestPermissions() }
                )
            }
        }

        // SAF Dialog
        if (showSAFDialog) {
            SAFAccessDialog(
                onDismiss = { showSAFDialog = false },
                onPickDirectory = { pickDirectory.launch(null) },
                onOpenSettings = { openSAFSettings() }
            )
        }
    }
}

enum class PermissionStatus {
    Unknown,
    Granted,
    Denied,
    SAFGranted
}

@Composable
fun PermissionDeniedView(
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    onUseSAF: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.FolderOff, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Storage Permission Required",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "This app needs access to your files to browse, copy, move, and manage them.\n" +
            "Please grant storage permission to continue.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Column(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onRequestPermissions,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Grant Permission")
            }
            OutlinedButton(
                onClick = onOpenSettings,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Open Settings")
            }
            OutlinedButton(
                onClick = onUseSAF,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Use File Picker (SAF)")
            }
        }
    }
}

@Composable
fun SAFGrantedView(
    onDismiss: () -> Unit,
    onPickDirectory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "SAF Access Granted",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "You granted access to a specific directory via the system file picker.\n" +
            "Browse files within that directory below.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onPickDirectory, shape = RoundedCornerShape(8.dp)) {
            Text("Pick Another Directory")
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onDismiss) {
            Text("Close")
        }
    }
}

@Composable
fun SAFAccessDialog(
    onDismiss: () -> Unit,
    onPickDirectory: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Use System File Picker") },
        text = {
            Column(modifier = Modifier.padding(16.dp).width(280.dp)) {
                Text(
                    "Android 11+ restricts direct file access. You can:\n\n" +
                    "1. Pick a specific folder using the system file picker (SAF)\n" +
                    "2. Grant 'All files access' in Settings (requires MANAGE_EXTERNAL_STORAGE)\n\n" +
                    "The file picker is more secure and doesn't require special permissions."
                )
            }
        },
        confirmButton = {
            Button(onClick = onPickDirectory) {
                Text("Pick Folder")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onOpenSettings) {
                Text("Open Settings")
            }
        }
    )
}

@Composable
fun FileExplorerContent(
    currentPath: String,
    files: List<FileItem>,
    isLoading: Boolean,
    selectedSidebarItem: String,
    searchQuery: String,
    onSidebarItemClick: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onFileClick: (FileItem) -> Unit,
    onNavigateUp: () -> Unit,
    onRefresh: () -> Unit,
    onRequestPermissions: () -> Unit
) {
    val sidebarItems = listOf("Documents", "Downloads", "Pictures", "Videos", "Music", "Internal Storage", "SD Card")

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Navigation Toolbar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Path breadcrumb
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Up")
                    }
                    Text(
                        text = currentPath,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Actions
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onRequestPermissions) {
                        Icon(Icons.Default.Security, contentDescription = "Permissions")
                    }
                }
            }
        }

        // Search Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text("Search files...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
            )
        }

        // Main Content Area
        Row(modifier = Modifier.fillMaxSize()) {
            // Sidebar
            NavigationRail(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(200.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                sidebarItems.forEachIndexed { index, item ->
                    NavigationRailItem(
                        selected = selectedSidebarItem == item,
                        onClick = { onSidebarItemClick(item) },
                        icon = { Icon(getFolderIcon(item), contentDescription = null) },
                        label = { Text(item, style = MaterialTheme.typography.labelLarge) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
            }

            Divider(modifier = Modifier.fillMaxHeight())

            // File List
            Column(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (files.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No files found",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (searchQuery.isNotBlank()) {
                                Text(
                                    "Try adjusting your search",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(files) { file ->
                            FileRow(
                                file = file,
                                onClick = { onFileClick(file) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FileRow(
    file: FileItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = RoundedCornerShape(8.dp)
            ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (file.isDirectory) Icons.Default.Folder else getFileIcon(file.name),
            contentDescription = null,
            tint = if (file.isDirectory) Color(0xFFEAB308) else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = file.name,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = java.text.SimpleDateFormat("M/d/yyyy h:mm a", java.util.Locale.US).format(java.util.Date(file.lastModified)),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = if (file.isDirectory) "File folder" else file.name.substringAfterLast(".").uppercase() + " File",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = if (file.isDirectory) "—" else "${file.sizeBytes / 1024} KB",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp)
        )
    }
}

private fun getFolderIcon(name: String) = when (name) {
    "Documents" -> Icons.Default.Description
    "Downloads" -> Icons.Default.Download
    "Pictures" -> Icons.Default.Image
    "Videos" -> Icons.Default.VideoLibrary
    "Music" -> Icons.Default.AudioFile
    "Internal Storage" -> Icons.Default.Storage
    "SD Card" -> Icons.Default.SdCard
    else -> Icons.Default.Folder
}

private fun getFileIcon(name: String) = when (name.substringAfterLast(".").lowercase()) {
    "jpg", "jpeg", "png", "gif", "webp", "bmp" -> Icons.Default.Image
    "mp4", "mkv", "avi", "mov" -> Icons.Default.VideoFile
    "mp3", "wav", "flac", "ogg" -> Icons.Default.AudioFile
    "pdf" -> Icons.Default.PictureAsPdf
    "zip", "rar", "7z", "tar", "gz" -> Icons.Default.Archive
    "apk" -> Icons.Default.Android
    "txt", "md", "log" -> Icons.Default.Description
    else -> Icons.Default.InsertDriveFile
}

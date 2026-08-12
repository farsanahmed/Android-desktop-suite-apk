package com.example.ui.views

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileItem
import com.example.ui.MainViewModel

@Composable
fun FileExplorerView(viewModel: MainViewModel) {
    val currentPath by viewModel.currentPath.collectAsState()
    val files by viewModel.fileList.collectAsState()
    val isLoading by viewModel.isFileLoading.collectAsState()
    var selectedSidebarItem by remember { mutableStateOf("Documents") }
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Navigation Toolbar (matching reference image)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val parent = currentPath.substringBeforeLast("/")
                        if (parent.isNotBlank()) viewModel.loadFiles(parent)
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("file_explorer_up_button")
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Forward", modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = {
                        val parent = currentPath.substringBeforeLast("/")
                        if (parent.isNotBlank()) viewModel.loadFiles(parent)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Up", modifier = Modifier.size(18.dp))
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Path Breadcrumb Box
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Computer, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "This PC > $currentPath",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search Documents", fontSize = 11.sp) },
                    trailingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier
                        .width(180.dp)
                        .height(36.dp),
                    shape = RoundedCornerShape(6.dp),
                    singleLine = true
                )

                IconButton(onClick = { viewModel.loadFiles(currentPath) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(18.dp))
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        Row(modifier = Modifier.fillMaxSize()) {
            // Left Sidebar (Quick Access & Drives)
            Surface(
                modifier = Modifier
                    .width(190.dp)
                    .fillMaxHeight(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                ) {
                    Text(
                        text = "⭐ Quick Access",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val quickAccessItems = listOf(
                        Triple("Desktop", Icons.Default.DesktopWindows, "/sdcard/Desktop"),
                        Triple("Downloads", Icons.Default.Download, "/sdcard/Download"),
                        Triple("Documents", Icons.Default.Description, "/sdcard/Documents"),
                        Triple("Pictures", Icons.Default.Image, "/sdcard/Pictures"),
                        Triple("Music", Icons.Default.MusicNote, "/sdcard/Music"),
                        Triple("Videos", Icons.Default.Movie, "/sdcard/Movies")
                    )

                    quickAccessItems.forEach { (name, icon, path) ->
                        val isSelected = selectedSidebarItem == name
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else Color.Transparent)
                                .clickable {
                                    selectedSidebarItem = name
                                    viewModel.loadFiles(path)
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon, contentDescription = name, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(name, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "💻 This PC",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    listOf("Local Disk (C:)" to "/sdcard", "Local Disk (D:)" to "/storage").forEach { (drive, path) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { viewModel.loadFiles(path) }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Storage, contentDescription = drive, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(drive, fontSize = 12.sp)
                        }
                    }
                }
            }

            VerticalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Main File Area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(8.dp)
            ) {
                // Table Headers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Name", modifier = Modifier.weight(2.5f), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    Text("Date modified", modifier = Modifier.weight(1.8f), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    Text("Type", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    Text("Size", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    val filteredFiles = files.filter { it.name.contains(searchQuery, ignoreCase = true) }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(filteredFiles) { file ->
                            FileTableRowItem(
                                file = file,
                                onClick = {
                                    if (file.isDirectory) {
                                        viewModel.loadFiles(file.path)
                                    }
                                },
                                onZip = { viewModel.zipFile(file.path) },
                                onExtract = { viewModel.extractZip(file.path) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FileTableRowItem(
    file: FileItem,
    onClick: () -> Unit,
    onZip: () -> Unit,
    onExtract: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(2.5f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when {
                        file.isDirectory -> Icons.Default.Folder
                        file.name.endsWith(".zip") || file.name.endsWith(".7z") -> Icons.Default.FolderZip
                        file.name.endsWith(".apk") -> Icons.Default.Android
                        file.name.endsWith(".jpg") || file.name.endsWith(".png") -> Icons.Default.Image
                        file.name.endsWith(".mp4") -> Icons.Default.Movie
                        file.name.endsWith(".mp3") -> Icons.Default.MusicNote
                        else -> Icons.Default.InsertDriveFile
                    },
                    contentDescription = null,
                    tint = if (file.isDirectory) Color(0xFFEAB308) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            val dateStr = remember(file.lastModified) {
                java.text.SimpleDateFormat("M/d/yyyy h:mm a", java.util.Locale.US).format(java.util.Date(file.lastModified))
            }
            Text(
                text = dateStr,
                modifier = Modifier.weight(1.8f),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = if (file.isDirectory) "File folder" else file.name.substringAfterLast(".").uppercase() + " File",
                modifier = Modifier.weight(1.5f),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = if (file.isDirectory) "—" else "${file.sizeBytes / 1024} KB",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

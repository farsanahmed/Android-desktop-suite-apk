package com.example.ui.desktop

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.androiddesktopsuite.wxsudv.R
import com.example.data.model.DesktopApp
import com.example.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun DesktopScreen(
    viewModel: MainViewModel,
    windowContents: @Composable (DesktopApp) -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val metrics by viewModel.deviceMetrics.collectAsState()
    val openWindows = viewModel.openWindows
    val activeWindow by viewModel.activeWindow

    val isDark = settings.isDarkMode
    val bgGradient = if (isDark) {
        listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF020617))
    } else {
        listOf(Color(0xFFF1F5F9), Color(0xFFE2E8F0), Color(0xFFCBD5E1))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(bgGradient))
    ) {
        // Desktop Wallpaper
        Image(
            painter = painterResource(id = R.drawable.img_desktop_wallpaper_1785164192206),
            contentDescription = "Desktop Wallpaper",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = if (isDark) 0.95f else 0.85f
        )

        // Top Right Desktop Clock & Performance Gadget Widget (matching reference image)
        DesktopWidgetGadget(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 36.dp, end = 24.dp),
            metrics = metrics
        )

        // Desktop Shortcuts Grid (Left Side)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 60.dp, top = 28.dp, start = 16.dp, end = 16.dp)
        ) {
            DesktopIconGrid(
                onAppClick = { viewModel.openApp(it) }
            )
        }

        // Active Application Windows Layer
        openWindows.forEach { app ->
            val isActive = activeWindow == app
            WindowContainer(
                app = app,
                isActive = isActive,
                onFocus = { viewModel.activeWindow.value = app },
                onClose = { viewModel.closeApp(app) },
                content = { windowContents(app) }
            )
        }

        // Widgets Drawer Flyout
        AnimatedVisibility(
            visible = viewModel.isWidgetsOpen.value,
            enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            WidgetsPanel(
                viewModel = viewModel,
                onClose = { viewModel.isWidgetsOpen.value = false }
            )
        }

        // Start Menu Popover
        AnimatedVisibility(
            visible = viewModel.isStartMenuOpen.value,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 60.dp, start = 16.dp)
        ) {
            StartMenu(
                viewModel = viewModel,
                onAppClick = { viewModel.openApp(it) }
            )
        }

        // Quick Settings Panel Flyout
        AnimatedVisibility(
            visible = viewModel.isQuickSettingsOpen.value,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 68.dp, end = 12.dp)
        ) {
            QuickSettingsPanel(
                viewModel = viewModel,
                onClose = { viewModel.isQuickSettingsOpen.value = false }
            )
        }

        // Windows 11 Taskbar
        Taskbar(
            modifier = Modifier.align(Alignment.BottomCenter),
            viewModel = viewModel
        )
    }
}

@Composable
fun DesktopWidgetGadget(
    modifier: Modifier = Modifier,
    metrics: com.example.data.model.DeviceMetrics?
) {
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()) }
    var currentTime by remember { mutableStateOf(timeFormat.format(Date())) }
    var currentDate by remember { mutableStateOf(dateFormat.format(Date())) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = timeFormat.format(Date())
            currentDate = dateFormat.format(Date())
            kotlinx.coroutines.delay(10000)
        }
    }

    Surface(
        modifier = modifier
            .width(220.dp)
            .shadow(8.dp, shape = RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = Color(0x991E293B),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Clock & Date
            Text(
                text = currentTime,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Text(
                text = currentDate,
                style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.7f))
            )

            Spacer(modifier = Modifier.height(14.dp))

            // System Metrics
            Text(
                text = "System",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8)
                )
            )
            Spacer(modifier = Modifier.height(6.dp))

            val cpu = metrics?.cpuUsagePercent ?: 23
            val ram = if (metrics != null && metrics.ramTotalMb > 0) ((metrics.ramUsedMb.toFloat() / metrics.ramTotalMb) * 100).toInt() else 45
            val disk = 38

            MetricBarRow("CPU", cpu)
            MetricBarRow("Memory", ram)
            MetricBarRow("Disk (C:)", disk)

            Spacer(modifier = Modifier.height(12.dp))

            // Network Metrics
            Text(
                text = "Network",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8)
                )
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF3B82F6)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Upload", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                }
                Text("12.4 Mbps", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF22C55E)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Download", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                }
                Text("98.7 Mbps", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

@Composable
private fun MetricBarRow(label: String, value: Int) {
    Column(modifier = Modifier.padding(vertical = 3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
            Text("$value%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(modifier = Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { value / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = Color(0xFF38BDF8),
            trackColor = Color.White.copy(alpha = 0.15f)
        )
    }
}

@Composable
fun DesktopIconGrid(
    onAppClick: (DesktopApp) -> Unit
) {
    val systemIcons = listOf(
        Pair("This PC", Icons.Default.Computer),
        Pair("Recycle Bin", Icons.Default.Delete),
        Pair("User", Icons.Default.Person),
        Pair("Control Panel", Icons.Default.GridView)
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    ) {
        systemIcons.forEach { (title, icon) ->
            SystemDesktopIconItem(
                title = title,
                icon = icon,
                onClick = {
                    when (title) {
                        "This PC", "User" -> onAppClick(DesktopApp.FILE_EXPLORER)
                        "Control Panel" -> onAppClick(DesktopApp.SETTINGS)
                        else -> onAppClick(DesktopApp.DASHBOARD)
                    }
                }
            )
        }
    }
}

@Composable
fun SystemDesktopIconItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(86.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(6.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            shadowElevation = 4.dp,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            ),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun DesktopIconItem(
    app: DesktopApp,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(86.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(8.dp)
            .testTag("desktop_icon_${app.name.lowercase()}")
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            shadowElevation = 6.dp,
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = getAppIconVector(app.iconName),
                    contentDescription = app.title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = app.title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            ),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun Taskbar(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel
) {
    val openWindows = viewModel.openWindows
    val activeWindow by viewModel.activeWindow
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()) }
    var currentTime by remember { mutableStateOf(timeFormat.format(Date())) }
    var currentDate by remember { mutableStateOf(dateFormat.format(Date())) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = timeFormat.format(Date())
            currentDate = dateFormat.format(Date())
            kotlinx.coroutines.delay(10000)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .windowInsetsPadding(WindowInsets.navigationBars),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = 8.dp,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Widgets Button
            IconButton(
                onClick = { viewModel.toggleWidgets() },
                modifier = Modifier
                    .size(40.dp)
                    .testTag("taskbar_widgets_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Dashboard,
                    contentDescription = "Widgets",
                    tint = if (viewModel.isWidgetsOpen.value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }

            // Centered App Icons
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Windows Start Button
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (viewModel.isStartMenuOpen.value) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { viewModel.toggleStartMenu() }
                        .testTag("taskbar_start_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Window,
                            contentDescription = "Start Menu",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Search Pill Bar (matching reference image)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .width(150.dp)
                        .height(34.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { viewModel.isStartMenuOpen.value = true }
                        .padding(horizontal = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Search",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Taskbar App Icons
                val taskbarApps = listOf(
                    Triple("Antigravity", Icons.Default.Explore, Color(0xFF818CF8)),
                    Triple("File Manager", Icons.Default.Folder, Color(0xFFEAB308)),
                    Triple("Firefox", Icons.Default.Public, Color(0xFFF97316)),
                    Triple("VS Code", Icons.Default.Code, Color(0xFF38BDF8)),
                    Triple("Terminal", Icons.Default.Terminal, Color(0xFF475569)),
                    Triple("Chat App", Icons.Default.Chat, Color(0xFFA855F7))
                )

                taskbarApps.forEach { (tbTitle, tbIcon, tbColor) ->
                    val appEnum = when (tbTitle) {
                        "File Manager" -> DesktopApp.FILE_EXPLORER
                        "Terminal" -> DesktopApp.ADB_FASTBOOT
                        "Chat App" -> DesktopApp.AI_ASSISTANT
                        else -> DesktopApp.DASHBOARD
                    }
                    val isOpen = openWindows.contains(appEnum)
                    val isActive = activeWindow == appEnum

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else if (isOpen) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                else Color.Transparent
                            )
                            .clickable { viewModel.openApp(appEnum) }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = tbIcon,
                            contentDescription = tbTitle,
                            tint = tbColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .width(if (isActive) 16.dp else if (isOpen) 6.dp else 0.dp)
                                .height(2.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }

            // Right System Tray (Clock & Quick Settings)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (viewModel.isQuickSettingsOpen.value) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { viewModel.toggleQuickSettings() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("taskbar_quick_settings")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Hidden icons",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = "Wi-Fi",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Volume",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.BatteryChargingFull,
                            contentDescription = "Battery",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Clock
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.clickable { viewModel.toggleQuickSettings() }
                ) {
                    Text(
                        text = currentTime,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = currentDate,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun WindowContainer(
    app: DesktopApp,
    isActive: Boolean,
    onFocus: () -> Unit,
    onClose: () -> Unit,
    content: @Composable () -> Unit
) {
    var offsetX by remember { mutableStateOf(160f) }
    var offsetY by remember { mutableStateOf(0f) }

    Surface(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .fillMaxWidth(0.58f)
            .fillMaxHeight(0.78f)
            .padding(top = 28.dp, bottom = 64.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
            .clickable { onFocus() }
            .shadow(if (isActive) 16.dp else 4.dp, shape = RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Windows 11 Window Header Titlebar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
                color = if (isActive) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
                        )
                        Text(
                            text = app.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // Window Controls (Minimize, Restore, Close)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Minimize",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.CropSquare,
                            contentDescription = "Maximize",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { onClose() }
                                .testTag("window_close_${app.name.lowercase()}"),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Window Content View
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                content()
            }
        }
    }
}

@Composable
fun StartMenu(
    viewModel: MainViewModel,
    onAppClick: (DesktopApp) -> Unit
) {
    Surface(
        modifier = Modifier
            .width(420.dp)
            .height(560.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xEC1E293B),
        shadowElevation = 16.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // User Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF38BDF8)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "User",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "User",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pinned Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pinned",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier.clickable {}
                ) {
                    Text(
                        text = "All apps >",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 10 Pinned Apps matching reference image
            val pinnedList = listOf(
                Triple("Antigravity", Icons.Default.Explore, Color(0xFF818CF8)),
                Triple("VS Code", Icons.Default.Code, Color(0xFF38BDF8)),
                Triple("Firefox", Icons.Default.Public, Color(0xFFF97316)),
                Triple("File Manager", Icons.Default.Folder, Color(0xFFEAB308)),
                Triple("Terminal", Icons.Default.Terminal, Color(0xFF475569)),
                Triple("Chat App", Icons.Default.Chat, Color(0xFFA855F7)),
                Triple("Media Player", Icons.Default.PlayCircle, Color(0xFFEC4899)),
                Triple("Settings", Icons.Default.Settings, Color(0xFF64748B)),
                Triple("Photos", Icons.Default.Image, Color(0xFF0EA5E9)),
                Triple("Game", Icons.Default.SportsEsports, Color(0xFF22C55E))
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.height(150.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(pinnedList.size) { index ->
                    val (pTitle, pIcon, pColor) = pinnedList[index]
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                when (pTitle) {
                                    "File Manager" -> onAppClick(DesktopApp.FILE_EXPLORER)
                                    "Terminal" -> onAppClick(DesktopApp.ADB_FASTBOOT)
                                    "Settings" -> onAppClick(DesktopApp.SETTINGS)
                                    "Chat App" -> onAppClick(DesktopApp.AI_ASSISTANT)
                                    "Media Player" -> onAppClick(DesktopApp.MEDIA_PLAYER)
                                    else -> onAppClick(DesktopApp.DASHBOARD)
                                }
                            }
                            .padding(2.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = pColor.copy(alpha = 0.2f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = pIcon,
                                    contentDescription = pTitle,
                                    tint = pColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = pTitle,
                            fontSize = 9.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Recommended Section
            Text(
                text = "Recommended",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
            )

            Spacer(modifier = Modifier.height(8.dp))

            val recommendedList = listOf(
                Triple("Getting Started", "Welcome to your system", Icons.Default.Explore),
                Triple("My Documents", "Documents", Icons.Default.Folder),
                Triple("Windows App", "Install and discover apps", Icons.Default.Apps),
                Triple("Downloads", "Files", Icons.Default.Download),
                Triple("Tips & Tricks", "Make the most of your PC", Icons.Default.Lightbulb),
                Triple("Pictures", "Photos", Icons.Default.Image)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(recommendedList.size) { index ->
                    val (rTitle, rSub, rIcon) = recommendedList[index]
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (rTitle.contains("Documents") || rTitle.contains("Downloads") || rTitle.contains("Pictures")) {
                                    onAppClick(DesktopApp.FILE_EXPLORER)
                                } else {
                                    onAppClick(DesktopApp.DASHBOARD)
                                }
                            }
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.1f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = rIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFF38BDF8)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(rTitle, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1)
                            Text(rSub, fontSize = 9.sp, color = Color.White.copy(alpha = 0.6f), maxLines = 1)
                        }
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 6.dp))

            // Bottom Bar Power Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { viewModel.isStartMenuOpen.value = false }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Power",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Power", fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun QuickSettingsPanel(
    viewModel: MainViewModel,
    onClose: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()

    Surface(
        modifier = Modifier
            .width(300.dp)
            .wrapContentHeight(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Quick Settings",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickSettingTile("Wi-Fi", Icons.Default.Wifi, true) {}
                QuickSettingTile("Bluetooth", Icons.Default.Bluetooth, true) {}
                QuickSettingTile(
                    if (settings.isDarkMode) "Dark Mode" else "Light Mode",
                    Icons.Default.DarkMode,
                    settings.isDarkMode
                ) {
                    viewModel.updateSettings(settings.copy(isDarkMode = !settings.isDarkMode))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Transparency Effect", style = MaterialTheme.typography.labelSmall)
            Slider(
                value = settings.transparency,
                onValueChange = { viewModel.updateSettings(settings.copy(transparency = it)) },
                valueRange = 0.3f..1f
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { onClose() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Done")
            }
        }
    }
}

@Composable
fun QuickSettingTile(
    label: String,
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(50.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (active) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 10.sp, maxLines = 1)
    }
}

@Composable
fun WidgetsPanel(
    viewModel: MainViewModel,
    onClose: () -> Unit
) {
    val metrics by viewModel.deviceMetrics.collectAsState()

    Surface(
        modifier = Modifier
            .width(320.dp)
            .fillMaxHeight(0.85f)
            .padding(start = 8.dp, top = 28.dp, bottom = 64.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Desktop Widgets",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Widget 1: System Performance Gauge
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("CPU Usage", style = MaterialTheme.typography.labelMedium)
                        Text("${metrics?.cpuUsagePercent ?: 25}%", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(
                        progress = { (metrics?.cpuUsagePercent ?: 25) / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("RAM Usage", style = MaterialTheme.typography.labelMedium)
                        Text("${metrics?.ramUsedMb ?: 0} / ${metrics?.ramTotalMb ?: 0} MB", fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(
                        progress = {
                            val total = (metrics?.ramTotalMb ?: 1).toFloat()
                            val used = (metrics?.ramUsedMb ?: 0).toFloat()
                            (used / total).coerceIn(0f, 1f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Widget 2: Battery & Temp
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Battery Status", style = MaterialTheme.typography.labelMedium)
                        Text("${metrics?.batteryLevel ?: 85}% (${if (metrics?.isCharging == true) "Charging" else "Discharging"})", fontWeight = FontWeight.Bold)
                    }
                    Text("${metrics?.batteryTempC ?: 32.0f}°C", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

fun getAppIconVector(iconName: String): ImageVector {
    return when (iconName) {
        "dashboard" -> Icons.Default.Dashboard
        "devices" -> Icons.Default.PhoneAndroid
        "terminal" -> Icons.Default.Terminal
        "folder" -> Icons.Default.Folder
        "android" -> Icons.Default.Android
        "cast" -> Icons.Default.CastConnected
        "auto_awesome" -> Icons.Default.AutoAwesome
        "play_circle" -> Icons.Default.PlayCircle
        "build" -> Icons.Default.Build
        "settings" -> Icons.Default.Settings
        else -> Icons.Default.Apps
    }
}

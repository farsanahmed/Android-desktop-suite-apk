package com.example.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.engine.AdbConnectionManager
import com.example.data.engine.AdbConnectionState
import com.example.ui.MainViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.receiveAsFlow

@Composable
fun AdbFastbootView(viewModel: MainViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("ADB Terminal", "Fastboot Studio", "Logcat Viewer")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = FontWeight.SemiBold) }
                )
            }
        }

        when (selectedTab) {
            0 -> AdbTerminalTab(viewModel)
            1 -> FastbootStudioTab(viewModel)
            2 -> LogcatViewerTab(viewModel)
        }
    }
}

@Composable
fun AdbTerminalTab(viewModel: MainViewModel) {
    var commandInput by remember { mutableStateOf("") }
    val history by viewModel.terminalHistory.collectAsState()

    val activeDevice by viewModel.adbConnectionManager.activeDevice.collectAsState()
    val connectionState by viewModel.adbConnectionManager.connectionState.collectAsState()
    val scope = rememberCoroutineScope()

    var showWirelessDialog by remember { mutableStateOf(false) }
    var wirelessIp by remember { mutableStateOf("") }
    var wirelessPort by remember { mutableStateOf("5555") }
    var pairingCode by remember { mutableStateOf("") }
    var showPairingDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Connection Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = when (connectionState) {
                                    is AdbConnectionState.Connected -> Color(0xFF22C55E)
                                    is AdbConnectionState.Connecting -> Color(0xFFF59E0B)
                                    is AdbConnectionState.Pairing -> Color(0xFF3B82F6)
                                    else -> Color(0xFFEF4444)
                                },
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (connectionState) {
                            is AdbConnectionState.Connected -> "${activeDevice?.model} (${activeDevice?.serial})",
                            is AdbConnectionState.Connecting -> "Connecting...",
                            is AdbConnectionState.Pairing -> "Pairing...",
                            is AdbConnectionState.Error -> "Error: ${connectionState.message}",
                            else -> "Disconnected"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { showWirelessDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Wireless ADB", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = { showPairingDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pair Device", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                viewModel.adbConnectionManager.disconnect()
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.UsbOff, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Disconnect", fontSize = 11.sp)
                    }
                }
            }
        }

        // Connection Dialogs
        if (showWirelessDialog) {
            WirelessConnectDialog(
                onDismiss = { showWirelessDialog = false },
                onConnect = { ip, port ->
                    scope.launch {
                        viewModel.adbConnectionManager.connectWireless(ip, port)
                    }
                }
            )
        }

        if (showPairingDialog) {
            PairingDialog(
                onDismiss = { showPairingDialog = false },
                onPair = { ip, port, code ->
                    scope.launch {
                        viewModel.adbConnectionManager.pairWireless(ip, port, code)
                    }
                }
            )
        }

        // Quick Action Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.runAdbCommand("adb devices") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("adb devices", fontSize = 11.sp)
            }
            Button(
                onClick = { viewModel.runAdbCommand("adb shell getprop") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("getprop", fontSize = 11.sp)
            }
            Button(
                onClick = { viewModel.runAdbCommand("adb shell pm list packages") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("list packages", fontSize = 11.sp)
            }
            IconButton(onClick = { viewModel.clearTerminal() }) {
                Icon(Icons.Default.Delete, contentDescription = "Clear")
            }
        }

        // Terminal Output Screen
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            color = Color(0xFF0F172A),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                reverseLayout = true
            ) {
                items(history) { entry ->
                    Column {
                        Text(
                            text = "> ${entry.command}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = entry.output,
                            fontFamily = FontFamily.Monospace,
                            color = if (entry.isError) Color(0xFFEF4444) else Color(0xFFE2E8F0),
                            fontSize = 12.sp
                        )
                        Divider(color = Color(0xFF1E293B), modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Command Input Box
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = commandInput,
                onValueChange = { commandInput = it },
                placeholder = { Text("Enter adb or shell command e.g. 'adb shell dumpsys battery'...", fontSize = 12.sp) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("adb_command_input"),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )
            Button(
                onClick = {
                    if (commandInput.isNotBlank()) {
                        viewModel.runAdbCommand(commandInput)
                        commandInput = ""
                    }
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("adb_send_button")
            ) {
                Icon(Icons.Default.Send, contentDescription = "Run")
            }
        }
    }
}

@Composable
fun WirelessConnectDialog(
    onDismiss: () -> Unit,
    onConnect: (String, Int) -> Unit
) {
    var ip by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("5555") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Connect Wireless ADB") },
        text = {
            Column(modifier = Modifier.padding(16.dp).width(280.dp)) {
                Text("Enter the IP address and port of the device with wireless debugging enabled.")
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = { Text("IP Address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("Port (default 5555)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (ip.isNotBlank()) {
                    onConnect(ip, port.toIntOrNull() ?: 5555)
                }
            }) {
                Text("Connect")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PairingDialog(
    onDismiss: () -> Unit,
    onPair: (String, Int, String) -> Unit
) {
    var ip by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("5555") }
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pair Wireless ADB") },
        text = {
            Column(modifier = Modifier.padding(16.dp).width(280.dp)) {
                Text("Enable 'Wireless debugging' in Developer Options, then tap 'Pair with pairing code' to get the IP, port, and 6-digit code.")
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = { Text("IP Address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("Port") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("6-digit Pairing Code") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (ip.isNotBlank() && code.isNotBlank()) {
                    onPair(ip, port.toIntOrNull() ?: 5555, code)
                }
            }) {
                Text("Pair")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun FastbootStudioTab(viewModel: MainViewModel) {
    var partition by remember { mutableStateOf("boot") }
    var imagePath by remember { mutableStateOf("boot.img") }
    var showUnlockWarning by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Fastboot Partition & Bootloader Studio",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Unavailable Notice
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Fastboot Unavailable Over Network ADB", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Fastboot requires:\n" +
                    "• Device in bootloader/fastboot mode (Power + Volume Down)\n" +
                    "• Direct USB connection to a host computer\n" +
                    "• Host running the fastboot binary (Android SDK Platform Tools)\n\n" +
                    "This app communicates via ADB over TCP, which only works when Android is booted. " +
                    "Fastboot operates in the bootloader before Android starts, so it cannot be accessed remotely.\n\n" +
                    "To use fastboot, connect your device via USB to a computer with Android SDK Platform Tools installed.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Fastboot Actions (shown but disabled)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { viewModel.runAdbCommand("fastboot devices") },
                shape = RoundedCornerShape(8.dp),
                enabled = false
            ) {
                Text("fastboot devices")
            }
            Button(
                onClick = { viewModel.runAdbCommand("fastboot getvar all") },
                shape = RoundedCornerShape(8.dp),
                enabled = false
            ) {
                Text("getvar all")
            }
            OutlinedButton(
                onClick = { showUnlockWarning = true },
                shape = RoundedCornerShape(8.dp),
                enabled = false,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
            ) {
                Text("Unlock Bootloader")
            }
        }
    }
}

@Composable
fun LogcatViewerTab(viewModel: MainViewModel) {
    var logLevel by remember { mutableStateOf("V") }
    var searchQuery by remember { mutableStateOf("") }
    var isPaused by remember { mutableStateOf(false) }
    val logLines = remember { mutableStateListOf<String>() }
    val scope = rememberCoroutineScope()

    val activeDevice by viewModel.adbConnectionManager.activeDevice.collectAsState()
    val connectionState by viewModel.adbConnectionManager.connectionState.collectAsState()
    val isConnected = connectionState is AdbConnectionState.Connected

    // Start/stop logcat based on connection state
    LaunchedEffect(key1 = isConnected, key2 = isPaused, key3 = logLevel, key4 = searchQuery) {
        if (isConnected && !isPaused) {
            scope.launch {
                viewModel.adbConnectionManager.startLogcat(logLevel, searchQuery) { line ->
                    // This callback approach won't work well with Compose
                    // We'll use the channel-based approach instead
                }
            }
        } else {
            viewModel.adbConnectionManager.stopLogcat()
        }
    }

    // Real-time logcat stream
    val logStream = remember {
        viewModel.adbConnectionManager.logcatStream.receiveAsFlow()
    }
    val logStreamCollected by logStream.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Logcat Stream", fontWeight = FontWeight.Bold)

            // Level filter
            OutlinedTextField(
                value = logLevel,
                onValueChange = { logLevel = it.uppercase() },
                label = { Text("Level") },
                modifier = Modifier.width(80.dp),
                singleLine = true
            )

            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
            )

            // Pause/Resume
            IconButton(
                onClick = {
                    isPaused = !isPaused
                    if (isPaused) {
                        viewModel.adbConnectionManager.pauseLogcat()
                    } else {
                        viewModel.adbConnectionManager.resumeLogcat()
                    }
                }
            ) {
                Icon(
                    if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (isPaused) "Resume" else "Pause"
                )
            }

            // Clear
            IconButton(
                onClick = {
                    scope.launch {
                        viewModel.adbConnectionManager.clearLogcat()
                        logLines.clear()
                    }
                }
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Clear")
            }

            // Refresh (fetch latest dump)
            IconButton(
                onClick = {
                    scope.launch {
                        val (output, _) = viewModel.adbConnectionManager.executeShellCommand("logcat -d -v brief *:$logLevel -t 200")
                        logLines.clear()
                        logLines.addAll(output.lines().filter { searchQuery.isBlank() || it.contains(searchQuery, ignoreCase = true) })
                    }
                }
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Connection status
        if (!isConnected) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "Not connected to a device. Use the ADB Terminal tab to connect via Wireless ADB or Pair Device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Logcat Output
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            color = Color(0xFF020617),
            shape = RoundedCornerShape(8.dp)
        ) {
            if (logLines.isEmpty() && logStreamCollected.isEmpty()) {
                Text(
                    text = "Connect to a device and start logcat stream to see output.\n\n" +
                           "Tip: Use level filter (V/D/I/W/E/F) and search to narrow results.",
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF64748B),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .wrapContentSize(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    reverseLayout = true
                ) {
                    items(logLines + logStreamCollected) { line ->
                        Text(
                            text = line,
                            fontFamily = FontFamily.Monospace,
                            color = getLogLineColor(line),
                            fontSize = 11.sp,
                            modifier = Modifier.fillMaxWidth().wrapContentWidth()
                        )
                    }
                }
            }
        }
    }
}

private fun getLogLineColor(line: String): Color {
    return when {
        line.contains(":E ") || line.contains(" E ") -> Color(0xFFEF4444)
        line.contains(":W ") || line.contains(" W ") -> Color(0xFFF59E0B)
        line.contains(":I ") || line.contains(" I ") -> Color(0xFF22C55E)
        line.contains(":D ") || line.contains(" D ") -> Color(0xFF3B82F6)
        line.contains(":V ") || line.contains(" V ") -> Color(0xFF94A3B8)
        else -> Color(0xFFE2E8F0)
    }
}

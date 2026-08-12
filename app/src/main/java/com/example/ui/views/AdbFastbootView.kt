package com.example.ui.views

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import kotlinx.coroutines.launch

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
    var wirelessIp by remember { mutableStateOf("192.168.1.105") }
    var wirelessPort by remember { mutableStateOf("5555") }

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
                                color = if (activeDevice != null) Color(0xFF22C55E) else Color(0xFFEF4444),
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (activeDevice != null) "${activeDevice?.model} (${activeDevice?.serial})" else "Disconnected",
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
                        onClick = {
                            scope.launch {
                                viewModel.adbConnectionManager.restartAdbServer()
                                viewModel.runAdbCommand("adb devices")
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restart Server", fontSize = 11.sp)
                    }
                }
            }
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

        // Quick Fastboot Actions
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { viewModel.runAdbCommand("fastboot devices") },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("fastboot devices")
            }
            Button(
                onClick = { viewModel.runAdbCommand("fastboot getvar all") },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("getvar all")
            }
            OutlinedButton(
                onClick = { showUnlockWarning = true },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Unlock Bootloader")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Flasher Controls
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Flash Image to Partition", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = partition,
                    onValueChange = { partition = it },
                    label = { Text("Partition Name (boot, system, recovery, vendor)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = imagePath,
                    onValueChange = { imagePath = it },
                    label = { Text("Image File Path (.img)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.runAdbCommand("fastboot flash $partition $imagePath") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Execute Fastboot Flash $partition")
                }
            }
        }

        if (showUnlockWarning) {
            AlertDialog(
                onDismissRequest = { showUnlockWarning = false },
                title = { Text("Warning: Bootloader Unlock") },
                text = { Text("Unlocking the bootloader will erase all user data on the target Android device and void system hardware security. Proceed?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.runAdbCommand("fastboot flashing unlock")
                            showUnlockWarning = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Confirm Unlock")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showUnlockWarning = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun LogcatViewerTab(viewModel: MainViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("System Logcat Stream", fontWeight = FontWeight.Bold)
            Button(
                onClick = { viewModel.runAdbCommand("adb logcat") },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Refresh Stream")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            color = Color(0xFF020617),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = """
                    --------- beginning of system
                    07-27 10:14:02.112 1204 1204 I ActivityManager: START u0 {act=android.intent.action.MAIN cat=[android.intent.category.LAUNCHER] pkg=com.aistudio.androiddesktopsuite} h=9812a1
                    07-27 10:14:02.180 1204 1250 D AndroidDesktopSuite: Window Manager Acrylic Backdrop Surface attached.
                    07-27 10:14:02.210 1204 1250 I SystemMonitor: Memory status updated: 3412 MB RAM / 64 GB Storage.
                    07-27 10:14:02.250 1204 1260 D AdbService: Daemon connected via local socket 5555.
                    07-27 10:14:02.300 1204 1260 I FastbootEngine: Ready for partition flash commands.
                    07-27 10:14:02.340 1204 1270 V GeminiAssistant: Engine initialized with REST endpoint.
                """.trimIndent(),
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF22C55E),
                fontSize = 11.sp,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

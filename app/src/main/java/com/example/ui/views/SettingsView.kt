package com.example.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.MainViewModel

@Composable
fun SettingsView(viewModel: MainViewModel) {
    val settings by viewModel.settings.collectAsState()
    val scrollState = rememberScrollState()

    var adbHost by remember { mutableStateOf(settings.adbHost) }
    var adbPort by remember { mutableStateOf(settings.adbPort.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Desktop Suite Personalization & Settings",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Appearance Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Windows 11 Theme Mode", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (settings.isDarkMode) "Dark Theme (Mica Dark)" else "Light Theme (Mica Light)")
                    Switch(
                        checked = settings.isDarkMode,
                        onCheckedChange = {
                            viewModel.updateSettings(settings.copy(isDarkMode = it))
                        }
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                Text("Accent Color Palette", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                val accents = listOf(
                    "#0078D4" to Color(0xFF0078D4),
                    "#10B981" to Color(0xFF10B981),
                    "#EF4444" to Color(0xFFEF4444),
                    "#A855F7" to Color(0xFFA855F7),
                    "#F59E0B" to Color(0xFFF59E0B),
                    "#38BDF8" to Color(0xFF38BDF8)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    accents.forEach { (hex, color) ->
                        val isSelected = settings.accentColorHex == hex
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    2.dp,
                                    if (isSelected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                                    CircleShape
                                )
                                .clickable {
                                    viewModel.updateSettings(settings.copy(accentColorHex = hex))
                                }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Network ADB Config Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Wireless ADB Connection Config", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = adbHost,
                    onValueChange = { adbHost = it },
                    label = { Text("ADB Daemon IP / Host") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = adbPort,
                    onValueChange = { adbPort = it },
                    label = { Text("Wireless Debugging Port") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        viewModel.updateSettings(
                            settings.copy(
                                adbHost = adbHost,
                                adbPort = adbPort.toIntOrNull() ?: 5555
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Save Connection Config")
                }
            }
        }
    }
}

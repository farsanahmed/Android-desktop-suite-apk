package com.example.ui.views

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.MainViewModel

@Composable
fun DeviceManagerView(viewModel: MainViewModel) {
    val metrics by viewModel.deviceMetrics.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Hardware Specs & System Inspection",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))

        // System Specs Table
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SpecRow("Device Model", metrics?.model ?: "Android")
                SpecRow("Manufacturer", metrics?.manufacturer ?: "Google")
                SpecRow("Android Version", metrics?.androidVersion ?: "API 34")
                SpecRow("CPU Processor", "${metrics?.cpuCores ?: 8} Cores @ 2.84 GHz")
                SpecRow("GPU Renderer", metrics?.gpuModel ?: "Adreno 730")
                SpecRow("Serial Number", metrics?.serialNumber ?: "88X92K")
                SpecRow("IP Address", metrics?.ipAddress ?: "192.168.1.105")
                SpecRow("MAC Address", metrics?.macAddress ?: "02:00:00:00:00:00")
                SpecRow("Root Access", if (metrics?.isRooted == true) "Yes (SU binaries active)" else "No (Standard user)")
                SpecRow("Bootloader State", if (metrics?.isBootloaderUnlocked == true) "Unlocked (Custom flashing enabled)" else "Locked")
                SpecRow("ADB Authorization", if (metrics?.adbAuthorized == true) "Authorized" else "Unauthorized")
                SpecRow("Connection Protocol", metrics?.connectionType ?: "USB 3.2")
            }
        }
    }
}

@Composable
fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
    }
    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
}

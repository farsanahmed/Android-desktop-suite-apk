package com.example.ui.views

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DesktopApp
import com.example.ui.MainViewModel

@Composable
fun DashboardView(viewModel: MainViewModel) {
    val metrics by viewModel.deviceMetrics.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Hero Device Connection Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = "CONNECTED VIA USB 3.2",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "${metrics?.manufacturer ?: "Android"} ${metrics?.model ?: "Workstation"}",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Serial: ${metrics?.serialNumber ?: "88X92K019381"} • IP: ${metrics?.ipAddress ?: "192.168.1.105"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = Icons.Default.Smartphone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Launch Studio Tools
        Text(
            text = "Quick Launch Modules",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickLaunchCard("ADB Studio", Icons.Default.Terminal, Modifier.weight(1f)) {
                viewModel.openApp(DesktopApp.ADB_FASTBOOT)
            }
            QuickLaunchCard("File Manager", Icons.Default.Folder, Modifier.weight(1f)) {
                viewModel.openApp(DesktopApp.FILE_EXPLORER)
            }
            QuickLaunchCard("Mirroring", Icons.Default.CastConnected, Modifier.weight(1f)) {
                viewModel.openApp(DesktopApp.REMOTE_DESKTOP)
            }
            QuickLaunchCard("AI Assistant", Icons.Default.AutoAwesome, Modifier.weight(1f)) {
                viewModel.openApp(DesktopApp.AI_ASSISTANT)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Metrics Grid Cards
        Text(
            text = "System Telemetry & Specs",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricStatCard(
                title = "RAM Memory",
                value = "${metrics?.ramUsedMb ?: 0} MB",
                subtitle = "of ${metrics?.ramTotalMb ?: 0} MB",
                icon = Icons.Default.Memory,
                modifier = Modifier.weight(1f)
            )
            MetricStatCard(
                title = "Storage",
                value = "${metrics?.storageUsedGb ?: 0} GB",
                subtitle = "of ${metrics?.storageTotalGb ?: 0} GB",
                icon = Icons.Default.Storage,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricStatCard(
                title = "Battery",
                value = "${metrics?.batteryLevel ?: 85}%",
                subtitle = if (metrics?.isCharging == true) "Charging (${metrics?.batteryTempC}°C)" else "Discharging (${metrics?.batteryTempC}°C)",
                icon = Icons.Default.BatteryChargingFull,
                modifier = Modifier.weight(1f)
            )
            MetricStatCard(
                title = "Root & Security",
                value = if (metrics?.isRooted == true) "Rooted (SU)" else "Standard Security",
                subtitle = "ADB Authorized",
                icon = Icons.Default.Security,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun QuickLaunchCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold))
        }
    }
}

@Composable
fun MetricStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

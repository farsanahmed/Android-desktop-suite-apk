package com.example.ui.views

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel

@Composable
fun UtilitiesView(viewModel: MainViewModel) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    val subTabs = listOf("Quick Notes", "Ping Diagnostic", "Hasher & Checksum", "Password Gen", "Clipboard")

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = selectedSubTab) {
            subTabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedSubTab == idx,
                    onClick = { selectedSubTab = idx },
                    text = { Text(title, fontSize = 12.sp) }
                )
            }
        }

        when (selectedSubTab) {
            0 -> NotesTab(viewModel)
            1 -> NetworkPingTab(viewModel)
            2 -> HasherTab()
            3 -> PasswordGenTab()
            4 -> ClipboardTab(viewModel)
        }
    }
}

@Composable
fun NotesTab(viewModel: MainViewModel) {
    val notes by viewModel.notes.collectAsState()
    var noteTitle by remember { mutableStateOf("") }
    var noteContent by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                OutlinedTextField(
                    value = noteTitle,
                    onValueChange = { noteTitle = it },
                    label = { Text("Note Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = noteContent,
                    onValueChange = { noteContent = it },
                    label = { Text("Note Content...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (noteContent.isNotBlank() || noteTitle.isNotBlank()) {
                            viewModel.addNote(noteTitle, noteContent, "#0284C7")
                            noteTitle = ""
                            noteContent = ""
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.End)
                        .testTag("save_note_button"),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Save Note")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(notes) { note ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(note.title, fontWeight = FontWeight.Bold)
                            Text(note.content, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { viewModel.deleteNote(note.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NetworkPingTab(viewModel: MainViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = viewModel.pingHost.value,
                onValueChange = { viewModel.pingHost.value = it },
                label = { Text("Host or IP") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Button(
                onClick = { viewModel.runNetworkPing() },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Test Ping")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = viewModel.pingOutput.value,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(12.dp),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun HasherTab() {
    var inputText by remember { mutableStateOf("AndroidDesktopSuite2026") }
    val md5Hash = remember(inputText) {
        java.security.MessageDigest.getInstance("MD5").digest(inputText.toByteArray()).joinToString("") { "%02x".format(it) }
    }
    val sha256Hash = remember(inputText) {
        java.security.MessageDigest.getInstance("SHA-256").digest(inputText.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("Input String for Hash Analysis") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        Text("MD5 Checksum:", fontWeight = FontWeight.Bold)
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text(md5Hash, fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.padding(8.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("SHA-256 Checksum:", fontWeight = FontWeight.Bold)
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text(sha256Hash, fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.padding(8.dp))
        }
    }
}

@Composable
fun PasswordGenTab() {
    var passwordLength by remember { mutableFloatStateOf(16f) }
    var generatedPass by remember { mutableStateOf("K9#xL2\$vM8!qP4#w") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Text("Password Length: ${passwordLength.toInt()}", fontWeight = FontWeight.Bold)
        Slider(
            value = passwordLength,
            onValueChange = { passwordLength = it },
            valueRange = 8f..32f
        )
        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()"
                generatedPass = (1..passwordLength.toInt()).map { chars.random() }.joinToString("")
            },
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Generate Secure Password")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = generatedPass,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun ClipboardTab(viewModel: MainViewModel) {
    val clips by viewModel.clipboardHistory.collectAsState()
    var newClipText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newClipText,
                onValueChange = { newClipText = it },
                placeholder = { Text("Type text to sync to Clipboard...", fontSize = 12.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Button(
                onClick = {
                    if (newClipText.isNotBlank()) {
                        viewModel.addClipboardText(newClipText)
                        newClipText = ""
                    }
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Sync")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(clips) { clip ->
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = clip.text,
                        modifier = Modifier.padding(12.dp),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

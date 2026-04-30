package com.ashasaathi.ui.screens.diary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ashasaathi.ui.components.voice.VoiceFAB
import com.ashasaathi.ui.strings.appStrings
import com.ashasaathi.ui.theme.*
import com.ashasaathi.ui.viewmodel.DiaryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(
    navController: NavController,
    vm: DiaryViewModel = hiltViewModel()
) {
    val entries by vm.entries.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var newEntry by remember { mutableStateOf("") }
    val s = appStrings()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.diaryTitle) },
                navigationIcon = { IconButton({ navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Saffron, titleContentColor = Color.White)
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                VoiceFAB()
                Spacer(Modifier.height(8.dp))
                FloatingActionButton(onClick = { showAddDialog = true }, containerColor = Teal) {
                    Icon(Icons.Default.Add, null, tint = Color.White)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (entries.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📔", style = MaterialTheme.typography.headlineLarge)
                            Text(s.diaryEmpty, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                            Text("Write or record your daily notes", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }
            }
            items(entries) { entry ->
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(entry.date, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(entry.content, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false; newEntry = "" },
            title = { Text(s.diaryNewEntry) },
            text = {
                OutlinedTextField(
                    value = newEntry,
                    onValueChange = { newEntry = it },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    placeholder = { Text(s.diaryHint) }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.addEntry(newEntry)
                    showAddDialog = false
                    newEntry = ""
                }, enabled = newEntry.isNotBlank()) { Text(s.diarySave) }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false; newEntry = "" }) { Text(s.diaryCancel) } }
        )
    }
}

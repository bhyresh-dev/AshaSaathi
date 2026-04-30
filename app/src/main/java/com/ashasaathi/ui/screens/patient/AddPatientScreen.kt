package com.ashasaathi.ui.screens.patient

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ashasaathi.ui.strings.appStrings
import com.ashasaathi.ui.theme.Saffron
import com.ashasaathi.ui.viewmodel.AddPatientViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPatientScreen(
    navController: NavController,
    vm: AddPatientViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val s = appStrings()

    var name         by remember { mutableStateOf("") }
    var dob          by remember { mutableStateOf("") }
    var gender       by remember { mutableStateOf("F") }
    var phone        by remember { mutableStateOf("") }
    var village      by remember { mutableStateOf("") }
    var isPregnant   by remember { mutableStateOf(false) }
    var isChild      by remember { mutableStateOf(false) }
    var hasTB        by remember { mutableStateOf(false) }
    var isElderly    by remember { mutableStateOf(false) }
    var nikshayId    by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(when(s.save) { "Save" -> "Add Patient"; "ಉಳಿಸಿ" -> "ರೋಗಿ ಸೇರಿಸಿ"; else -> "मरीज़ जोड़ें" }, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Saffron, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text(s.patientName) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = dob, onValueChange = { dob = it },
                label = { Text("${s.patientAge} (DOB: yyyy-MM-dd)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text("1990-01-15") }
            )

            // Gender
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(s.patientGender, style = MaterialTheme.typography.bodyMedium)
                listOf("F" to "Female / महिला", "M" to "Male / पुरुष", "OTHER" to "Other").forEach { (code, label) ->
                    FilterChip(
                        selected = gender == code,
                        onClick = { gender = code },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            OutlinedTextField(
                value = phone, onValueChange = { phone = it },
                label = { Text(s.patientPhone) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
            OutlinedTextField(
                value = village, onValueChange = { village = it },
                label = { Text("Village / गाँव") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )

            // Category flags
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Category", style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isPregnant, onCheckedChange = { isPregnant = it })
                        Text(s.tagPregnant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isChild, onCheckedChange = { isChild = it })
                        Text(s.tagChild + " (under 5)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isElderly, onCheckedChange = { isElderly = it })
                        Text(s.tagElderly)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = hasTB, onCheckedChange = { hasTB = it })
                        Text("💊 TB / DOTS")
                    }
                }
            }

            if (hasTB) {
                OutlinedTextField(
                    value = nikshayId, onValueChange = { nikshayId = it },
                    label = { Text("Nikshay ID") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
            }

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    vm.savePatient(
                        name = name, dob = dob, gender = gender,
                        phone = phone, village = village,
                        isPregnant = isPregnant, isChildUnder5 = isChild,
                        hasTB = hasTB, isElderly = isElderly,
                        nikshayId = nikshayId,
                        onComplete = { navController.popBackStack() }
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = name.isNotBlank() && dob.isNotBlank() && !state.saving,
                colors = ButtonDefaults.buttonColors(containerColor = Saffron)
            ) {
                if (state.saving) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(s.save, color = Color.White)
                }
            }
        }
    }
}

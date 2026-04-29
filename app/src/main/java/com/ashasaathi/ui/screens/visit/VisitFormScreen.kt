package com.ashasaathi.ui.screens.visit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ashasaathi.ui.theme.Primary
import com.ashasaathi.ui.viewmodel.VisitFormViewModel
import com.ashasaathi.ui.components.voice.VoiceFAB

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitFormScreen(
    navController: NavController,
    vm: VisitFormViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    val visitTypes = listOf("ANC", "PNC", "IMMUNISATION", "TB_DOTS", "ELDERLY", "FAMILY_PLANNING", "GENERAL")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("विजिट दर्ज करें") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary, titleContentColor = Color.White)
            )
        },
        floatingActionButton = {
            VoiceFAB(onTranscript = { vm.onVoiceTranscript(it) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Visit Type
            Text("विजिट प्रकार / Visit Type", style = MaterialTheme.typography.titleSmall)
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = state.visitType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Visit Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    visitTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = { vm.onVisitTypeChange(type); expanded = false }
                        )
                    }
                }
            }

            // Vitals
            Text("वाइटल्स / Vitals", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.bpSystolic,
                    onValueChange = vm::onBpSystolicChange,
                    label = { Text("BP Sys") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.bpDiastolic,
                    onValueChange = vm::onBpDiastolicChange,
                    label = { Text("BP Dia") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = state.weight, onValueChange = vm::onWeightChange, label = { Text("Weight kg") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                OutlinedTextField(value = state.hemoglobin, onValueChange = vm::onHemoglobinChange, label = { Text("Hb g/dL") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = state.spo2, onValueChange = vm::onSpo2Change, label = { Text("SpO2 %") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(value = state.temperature, onValueChange = vm::onTemperatureChange, label = { Text("Temp °C") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
            }

            // Fever & Cough
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked = state.hasFever, onCheckedChange = vm::onFeverChange)
                Text("बुखार है / Has Fever")
            }

            // Clinical Notes
            OutlinedTextField(
                value = state.clinicalNotes,
                onValueChange = vm::onNotesChange,
                label = { Text("Clinical Notes / टिप्पणियाँ") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                placeholder = { Text("Voice transcript will appear here...") }
            )

            // Referral
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked = state.referralNeeded, onCheckedChange = vm::onReferralChange)
                Text("रेफरल जरूरी / Referral Needed")
            }
            if (state.referralNeeded) {
                OutlinedTextField(
                    value = state.referralNote,
                    onValueChange = vm::onReferralNoteChange,
                    label = { Text("Referral Reason / कारण") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(
                onClick = { vm.saveVisit { navController.popBackStack() } },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !state.saving,
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                if (state.saving) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("विजिट सेव करें / Save Visit")
            }
        }
    }
}

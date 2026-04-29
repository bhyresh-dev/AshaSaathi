package com.ashasaathi.ui.screens.visit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ashasaathi.data.model.RiskFlag
import com.ashasaathi.ui.components.SaffronGradient
import com.ashasaathi.ui.components.voice.VoiceFAB
import com.ashasaathi.ui.theme.*
import com.ashasaathi.ui.viewmodel.VisitFormViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitFormScreen(
    navController: NavController,
    vm: VisitFormViewModel = hiltViewModel()
) {
    val state    by vm.state.collectAsState()
    val patient  by vm.patient.collectAsState()
    var showRisk by remember { mutableStateOf(false) }

    LaunchedEffect(state.riskResult) {
        if (state.riskResult != null) showRisk = true
    }

    Scaffold(
        containerColor = WarmBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("विजिट दर्ज करें", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        patient?.let { Text(it.name, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f)) }
                    }
                },
                navigationIcon = {
                    IconButton({ navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Saffron)
            )
        },
        floatingActionButton = {
            VoiceFAB(onExtracted = { data ->
                data.bpSystolic?.let    { vm.onBpSystolicChange(it.toString()) }
                data.bpDiastolic?.let   { vm.onBpDiastolicChange(it.toString()) }
                data.weightKg?.let      { vm.onWeightChange(it.toString()) }
                data.hemoglobinGdL?.let { vm.onHemoglobinChange(it.toString()) }
                data.temperature?.let   { vm.onTemperatureChange(it.toString()) }
                data.spo2?.let          { vm.onSpo2Change(it.toString()) }
                data.fastingGlucose?.let{ vm.onFastingGlucoseChange(it.toString()) }
                data.hasFever?.let      { vm.onFeverChange(it) }
                data.coughDays?.let     { vm.onCoughDaysChange(it.toString()) }
                data.clinicalNotes?.let { if (it.isNotBlank()) vm.onNotesChange(it) }
            })
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Visit type selector
            VisitTypeSection(
                selected = state.visitType,
                onSelect = vm::onVisitTypeChange
            )

            // Vitals card
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("वाइटल्स / Vitals",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField("BP Sys (mmHg)", state.bpSystolic, vm::onBpSystolicChange, Modifier.weight(1f))
                        NumberField("BP Dia (mmHg)", state.bpDiastolic, vm::onBpDiastolicChange, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField("वजन / Weight (kg)", state.weight, vm::onWeightChange, Modifier.weight(1f), decimal = true)
                        NumberField("Hb (g/dL)", state.hemoglobin, vm::onHemoglobinChange, Modifier.weight(1f), decimal = true)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField("SpO2 (%)", state.spo2, vm::onSpo2Change, Modifier.weight(1f))
                        NumberField("तापमान / Temp (°C)", state.temperature, vm::onTemperatureChange, Modifier.weight(1f), decimal = true)
                    }
                }
            }

            // Pregnancy section
            if (patient?.isPregnant == true) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("🤰 गर्भावस्था विवरण",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = RiskAmber
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberField("IFA आज दिए", state.ifaToday, vm::onIFAChange, Modifier.weight(1f))
                            NumberField("फास्टिंग शुगर", state.fastingGlucose, vm::onFastingGlucoseChange, Modifier.weight(1f), decimal = true)
                        }

                        // IFA progress bar toward 180
                        val totalIfa = (patient?.ifaCumulativeCount ?: 0) + (state.ifaToday.toIntOrNull() ?: 0)
                        val progress = (totalIfa / 180f).coerceIn(0f, 1f)
                        Column {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("IFA प्रगति: $totalIfa / 180",
                                    style = MaterialTheme.typography.labelMedium)
                                Text("${(progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (progress >= 1f) RiskGreen else RiskAmber)
                            }
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                color = if (progress >= 1f) RiskGreen else RiskAmber,
                                trackColor = Color(0xFFE0E0E0)
                            )
                        }

                        // Urine protein
                        UrineDropdown(selected = state.urineProtein, onSelect = vm::onUrineProteinChange)
                        OutlinedTextField(
                            value = state.ogtt2hr,
                            onValueChange = vm::onOGTTChange,
                            label = { Text("OGTT 2hr (mg/dL)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                    }
                }
            }

            // Fever + cough
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("लक्षण / Symptoms",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = state.hasFever, onCheckedChange = vm::onFeverChange)
                        Text("बुखार / Fever", style = MaterialTheme.typography.bodyMedium)
                    }
                    AnimatedVisibility(state.hasFever || (state.coughDays.toIntOrNull() ?: 0) > 0) {
                        NumberField("खांसी के दिन / Cough Days", state.coughDays, vm::onCoughDaysChange, Modifier.fillMaxWidth())
                    }
                }
            }

            // Clinical notes
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("नोट्स / Clinical Notes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp))
                    OutlinedTextField(
                        value = state.clinicalNotes,
                        onValueChange = vm::onNotesChange,
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        placeholder = { Text("माइक बटन दबाकर बोलें या यहाँ लिखें...") }
                    )
                }
            }

            // Referral
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = state.referralNeeded, onCheckedChange = vm::onReferralChange)
                        Text("रेफरल जरूरी / Referral Needed",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium)
                    }
                    AnimatedVisibility(state.referralNeeded) {
                        OutlinedTextField(
                            value = state.referralNote,
                            onValueChange = vm::onReferralNoteChange,
                            label = { Text("रेफरल कारण / Reason") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = { vm.saveVisit { showRisk = true } },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                enabled = !state.saving,
                colors = ButtonDefaults.buttonColors(containerColor = Saffron),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (state.saving) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Save, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("विजिट सेव करें / Save Visit",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White)
                }
            }
        }
    }

    // ── Risk result modal ──────────────────────────────────────────────────────
    state.riskResult?.let { riskResult ->
        if (showRisk) {
            AlertDialog(
                onDismissRequest = { showRisk = false; navController.popBackStack() },
                containerColor = when (riskResult.level) {
                    "RED"    -> RiskRedSurface
                    "YELLOW" -> RiskAmberSurface
                    else     -> RiskGreenSurface
                },
                icon = {
                    Text(when (riskResult.level) { "RED" -> "🔴"; "YELLOW" -> "🟡"; else -> "🟢" },
                        style = MaterialTheme.typography.displaySmall)
                },
                title = {
                    Text(
                        when (riskResult.level) {
                            "RED"    -> "उच्च जोखिम\nHigh Risk"
                            "YELLOW" -> "मध्यम जोखिम\nModerate Risk"
                            else     -> "सामान्य\nNormal"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = when (riskResult.level) { "RED" -> RiskRed; "YELLOW" -> RiskAmber; else -> RiskGreen }
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        riskResult.flags.forEach { flag ->
                            FlagRow(flag)
                        }
                        if (riskResult.flags.isEmpty()) {
                            Text("कोई जोखिम नहीं मिला।\nNo risk factors found.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = RiskGreen)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showRisk = false; navController.popBackStack() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (riskResult.level) {
                                "RED" -> RiskRed; "YELLOW" -> RiskAmber; else -> RiskGreen
                            }
                        )
                    ) {
                        Text("ठीक है / OK", color = Color.White)
                    }
                }
            )
        }
    }
}

@Composable
private fun FlagRow(flag: RiskFlag) {
    val color = when (flag.severity) { "RED" -> RiskRed; "YELLOW" -> RiskAmber; else -> RiskGreen }
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("•", color = color, fontWeight = FontWeight.Bold)
        Text(flag.reasonHi,
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VisitTypeSection(selected: String, onSelect: (String) -> Unit) {
    val types = listOf("ANC","PNC","IMMUNISATION","TB_DOTS","ELDERLY","FAMILY_PLANNING","HBYC","GENERAL")
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("विजिट प्रकार / Visit Type",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 10.dp))
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = selected,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    types.forEach { t ->
                        DropdownMenuItem(
                            text = { Text(t) },
                            onClick = { onSelect(t); expanded = false }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UrineDropdown(selected: String, onSelect: (String) -> Unit) {
    val options = listOf("NIL","TRACE","+1","+2","+3")
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.ifBlank { "NIL" },
            onValueChange = {},
            readOnly = true,
            label = { Text("मूत्र प्रोटीन / Urine Protein") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(opt) }, onClick = { onSelect(opt); expanded = false })
            }
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    decimal: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, maxLines = 1) },
        modifier = modifier,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number
        ),
        singleLine = true,
        shape = RoundedCornerShape(10.dp)
    )
}

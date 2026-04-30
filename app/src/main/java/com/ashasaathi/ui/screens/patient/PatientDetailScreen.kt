package com.ashasaathi.ui.screens.patient

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ashasaathi.data.model.Visit
import com.ashasaathi.ui.navigation.Route
import com.ashasaathi.ui.components.InfoRow
import com.ashasaathi.ui.strings.appStrings
import com.ashasaathi.ui.theme.*
import com.ashasaathi.ui.viewmodel.PatientDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(
    navController: NavController,
    vm: PatientDetailViewModel = hiltViewModel()
) {
    val patient by vm.patient.collectAsState()
    val visits by vm.visits.collectAsState()
    val allLocalDots by com.ashasaathi.data.repository.LocalRecordsStore.dots.collectAsState()
    val patientDots = remember(allLocalDots, patient) {
        val name = patient?.name ?: return@remember emptyList()
        allLocalDots.filter { it.patientName.equals(name, ignoreCase = true) }
    }
    val allLocalVax by com.ashasaathi.data.repository.LocalRecordsStore.vaccines.collectAsState()
    val patientVax = remember(allLocalVax, patient) {
        val name = patient?.name ?: return@remember emptyList()
        allLocalVax.filter { it.childName.equals(name, ignoreCase = true) || it.motherName.equals(name, ignoreCase = true) }
    }
    val s = appStrings()

    val riskColor = when (patient?.currentRiskLevel) {
        "RED" -> RiskRed; "YELLOW" -> RiskAmber; else -> RiskGreen
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(patient?.name ?: "Patient") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        patient?.let { navController.navigate(Route.mcpCard(it.patientId)) }
                    }) { Icon(Icons.Default.CreditCard, "MCP Card", tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Saffron, titleContentColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { patient?.let { navController.navigate(Route.visitForm(it.patientId)) } },
                containerColor = Saffron
            ) { Icon(Icons.Default.Add, "New Visit", tint = Color.White) }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            patient?.let { p ->
                item {
                    // Risk banner
                    Box(
                        Modifier.fillMaxWidth().background(riskColor, RoundedCornerShape(12.dp)).padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            p.currentRiskLevel,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(s.patientPersonalInfo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Divider()
                            InfoRow(s.patientName, p.name)
                            InfoRow(s.patientAge, "${p.age ?: "-"} ${s.years}")
                            InfoRow(s.patientGender, p.gender)
                            p.phone?.let { InfoRow(s.patientPhone, it) }
                            p.rchMctsId?.let { InfoRow("RCH ID", it) }
                            p.bloodGroup?.let { InfoRow("Blood Group", it) }
                        }
                    }
                }
                if (p.isPregnant) item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(s.patientPregnancy, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Divider()
                            p.lmpDate?.let { InfoRow("LMP", it) }
                            p.edd?.let { InfoRow("EDD", it) }
                            p.gestationalAgeWeeks?.let { InfoRow("GA Weeks", it.toString()) }
                            p.trimester?.let { InfoRow("Trimester", "T$it") }
                        }
                    }
                }
                if (p.hasTB) item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                        Column(Modifier.padding(16.dp)) {
                            Text(s.patientTBDots, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            p.lastVisitDate?.let {
                                Spacer(Modifier.height(4.dp))
                                InfoRow("Last Visit", it)
                            }
                        }
                    }
                }
                if (patientDots.isNotEmpty()) {
                    item {
                        Text("DOTS Records (${patientDots.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(patientDots, key = { "dots_${it.id}" }) { rec ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (rec.dotsTaken) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("💊 DOTS · ${rec.date}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    if (rec.sideEffects.isNotBlank()) Text("Side effects: ${rec.sideEffects}", style = MaterialTheme.typography.bodySmall, color = RiskRed)
                                }
                                Text(if (rec.dotsTaken) "✓ Taken" else "✗ Missed",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (rec.dotsTaken) RiskGreen else RiskRed,
                                    fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                if (patientVax.isNotEmpty()) {
                    item {
                        Text("Vaccinations (${patientVax.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(patientVax, key = { "vax_${it.id}" }) { rec ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2F1)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("💉 ${rec.vaccineName} · ${rec.date}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("✓ Done", style = MaterialTheme.typography.labelMedium, color = Teal, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                item {
                    Text(s.patientVisitHistory, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
            if (visits.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(s.patientNoVisits, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            }
            items(visits, key = { it.visitId }) { visit ->
                VisitHistoryCard(visit)
            }
        }
    }
}

@Composable
fun VisitHistoryCard(visit: Visit) {
    val s = appStrings()
    val riskColor = when (visit.riskLevel) { "RED" -> RiskRed; "YELLOW" -> RiskAmber; else -> RiskGreen }
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(riskColor))
            Column(Modifier.padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(visit.visitType, style = MaterialTheme.typography.labelLarge, color = Saffron, fontWeight = FontWeight.Bold)
                    Text(visit.visitDate, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                if (visit.clinicalNotes.isNotBlank()) {
                    Text(visit.clinicalNotes, style = MaterialTheme.typography.bodySmall, maxLines = 2, modifier = Modifier.padding(top = 4.dp))
                }
                if (visit.referralNeeded) {
                    Text(s.patientReferralNeeded, style = MaterialTheme.typography.labelSmall, color = RiskRed, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

package com.ashasaathi.ui.screens.vaccination

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ashasaathi.data.repository.VaccineScheduleEntry
import com.ashasaathi.data.repository.VaccineStatus
import com.ashasaathi.ui.components.RiskBadge
import com.ashasaathi.ui.screens.home.AppBottomBar
import com.ashasaathi.ui.strings.appStrings
import com.ashasaathi.ui.theme.*
import com.ashasaathi.ui.viewmodel.VaccinationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaccinationScreen(
    navController: NavController,
    vm: VaccinationViewModel = hiltViewModel()
) {
    val patientsWithSchedules by vm.patientsWithSchedules.collectAsState()
    val ficCount  by vm.ficCount.collectAsState()
    val loading   by vm.loading.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val s = appStrings()

    Scaffold(
        containerColor = WarmBackground,
        bottomBar = { AppBottomBar(navController) },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(s.vaccinationTitle, color = Color.White) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Saffron)
                )
                // FIC badge
                if (!loading) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(SaffronDark)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(s.vaccineFIC.format(ficCount), style = MaterialTheme.typography.labelLarge, color = Color.White)
                    }
                }
                TabRow(selectedTabIndex = selectedTab, containerColor = Teal, contentColor = Color.White) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(s.vaccineDueTab) })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(s.vaccineMissedTab) })
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text(s.vaccineAllTab) })
                }
            }
        }
    ) { padding ->
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Saffron)
            }
        } else if (patientsWithSchedules.isEmpty()) {
            DemoVaccinationContent(Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                patientsWithSchedules.forEach { (patient, schedule) ->
                    val filtered = when (selectedTab) {
                        0 -> schedule.filter { it.status == VaccineStatus.DUE_TODAY }
                        1 -> schedule.filter { it.status == VaccineStatus.MISSED }
                        else -> schedule
                    }
                    if (filtered.isEmpty()) return@forEach

                    item(key = patient.patientId) {
                        PatientVaccineHeader(
                            name = patient.name,
                            age = patient.age,
                            ficStatus = vm.computeFIC(schedule),
                            cicStatus = vm.computeCIC(schedule)
                        )
                    }
                    items(filtered, key = { it.vaccine.id }) { entry ->
                        VaccineEntryCard(
                            entry = entry,
                            onMark = { vm.markAdministered(patient.patientId, entry) }
                        )
                    }
                    item { Spacer(Modifier.height(4.dp)) }
                }
            }
        }
    }
}

@Composable
private fun PatientVaccineHeader(name: String, age: Int?, ficStatus: Boolean, cicStatus: Boolean) {
    val s = appStrings()
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            age?.let { Text(s.vaccineAgeLabel.format(it), style = MaterialTheme.typography.bodySmall, color = TextSecondary) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (ficStatus) StatusPill("FIC ✓", RiskGreen)
            if (cicStatus) StatusPill("CIC ✓", Teal)
            if (!ficStatus) StatusPill("FIC ✗", RiskRed)
        }
    }
}

@Composable
private fun StatusPill(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(100)) {
        Text(text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun VaccineEntryCard(
    entry: VaccineScheduleEntry,
    onMark: () -> Unit
) {
    val s = appStrings()
    val (dotColor, bg, statusText) = when (entry.status) {
        VaccineStatus.ADMINISTERED -> Triple(RiskGreen,   RiskGreenSurface,   s.vaccineGiven)
        VaccineStatus.DUE_TODAY    -> Triple(RiskAmber,   RiskAmberSurface,   s.vaccineGiveToday)
        VaccineStatus.MISSED       -> Triple(RiskRed,     RiskRedSurface,     s.vaccineMissedDays.format(entry.daysOverdue))
        VaccineStatus.UPCOMING     -> Triple(TextSecondary, WarmBackground,   s.vaccineUpcoming)
    }

    // Pulse for DUE_TODAY
    val pulse by rememberInfiniteTransition(label = "vax").animateFloat(
        initialValue = 1f,
        targetValue  = if (entry.status == VaccineStatus.DUE_TODAY) 1.04f else 1f,
        animationSpec = infiniteRepeatable(tween(if (entry.status == VaccineStatus.DUE_TODAY) 800 else 999999), RepeatMode.Reverse),
        label = "vaxScale"
    )

    Card(
        modifier = Modifier.fillMaxWidth().scale(pulse),
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(if (entry.status == VaccineStatus.DUE_TODAY) 4.dp else 1.dp)
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(dotColor))
            Column(Modifier.weight(1f)) {
                Text(entry.vaccine.nameHi, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("${entry.vaccine.nameEn} · ${entry.vaccine.dose}",
                    style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text(statusText, style = MaterialTheme.typography.labelSmall, color = dotColor)
                Text("${entry.vaccine.site} · ${entry.vaccine.route}",
                    style = MaterialTheme.typography.labelSmall, color = TextHint)
            }
            if (entry.status == VaccineStatus.DUE_TODAY || entry.status == VaccineStatus.MISSED) {
                Button(
                    onClick = onMark,
                    colors = ButtonDefaults.buttonColors(containerColor = Teal),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(s.vaccineMarkGiven, color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
            } else if (entry.status == VaccineStatus.ADMINISTERED) {
                entry.record?.administeredDate?.let { date ->
                    Text(date.take(10),
                        style = MaterialTheme.typography.labelSmall,
                        color = RiskGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DemoVaccinationContent(modifier: Modifier = Modifier) {
    val demoChildren = listOf(
        Triple("Aryan Kumar (8m)", "BCG, OPV1, Hep-B due", "🔴 Overdue"),
        Triple("Priya Sharma (4m)", "OPV2, Penta2 due", "🟡 Due Today"),
        Triple("Rahul Singh (12m)", "MR, JE due", "🟡 Due Soon"),
        Triple("Kavya Devi (18m)", "DPT Booster due", "🟢 On Track"),
    )
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("4 children · Sample data", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        itemsIndexed(demoChildren) { _, (name, vaccines, status) ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(status, style = MaterialTheme.typography.labelSmall)
                    }
                    Text(vaccines, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }
    }
}

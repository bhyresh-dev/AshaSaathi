package com.ashasaathi.ui.screens.tb

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ashasaathi.ui.strings.appStrings
import com.ashasaathi.ui.theme.*
import com.ashasaathi.ui.viewmodel.TBDotsViewModel
import com.ashasaathi.ui.viewmodel.TBPatientDisplay
import com.ashasaathi.ui.LocalAppLanguage
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TBDotsScreen(
    navController: NavController,
    vm: TBDotsViewModel = hiltViewModel()
) {
    val patients by vm.tbPatients.collectAsState()
    val localDots by com.ashasaathi.data.repository.LocalRecordsStore.dots.collectAsState()
    var selectedPatient by remember { mutableStateOf<TBPatientDisplay?>(null) }
    val s = appStrings()

    Scaffold(
        containerColor = WarmBackground,
        topBar = {
            TopAppBar(
                title = { Text(s.tbTitle, color = Color.White) },
                navigationIcon = { IconButton({ navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                }},
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Saffron)
            )
        }
    ) { padding ->
        if (patients.isEmpty() && localDots.isEmpty()) {
            DemoTBContent(Modifier.padding(padding))
        } else if (patients.isEmpty()) {
            LazyColumn(
                Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text("${localDots.size} DOTS record${if (localDots.size != 1) "s" else ""} today",
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                items(localDots, key = { it.id }) { rec ->
                    LocalDotsCard(rec)
                }
            }
        } else {
            LazyColumn(
                Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(patients, key = { it.patientId }) { patient ->
                    TBPatientCard(
                        patient = patient,
                        onOpenCalendar = { selectedPatient = patient },
                        onRecordDots = { vm.recordDots(patient.patientId) }
                    )
                }
            }
        }
    }

    // Calendar modal
    selectedPatient?.let { patient ->
        DOTSCalendarDialog(
            patient = patient,
            dotsCalendar = vm.getDotsCalendar(patient.patientId),
            onMarkDay = { date, status -> vm.markDotsDay(patient.patientId, date, status) },
            onDismiss = { selectedPatient = null }
        )
    }
}

@Composable
private fun TBPatientCard(
    patient: TBPatientDisplay,
    onOpenCalendar: () -> Unit,
    onRecordDots: () -> Unit
) {
    val s = appStrings()
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (patient.dotsDue) RiskRedSurface else Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(patient.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Nikshay: ${patient.nikshayId}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    patient.dotsRegimen?.let {
                        Text("Regimen: $it", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
                // Adherence ring
                AdherenceRing(patient.adherencePercent)
            }

            // Today status
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (patient.dotsDue) {
                    Surface(color = RiskRed.copy(alpha = 0.1f), shape = RoundedCornerShape(100)) {
                        Text(s.tbDotsDue,
                            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall, color = RiskRed, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Surface(color = RiskGreen.copy(alpha = 0.1f), shape = RoundedCornerShape(100)) {
                        Text(s.tbDotsGiven,
                            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall, color = RiskGreen)
                    }
                }
                // DBT status
                if (patient.dbtThisMonth) {
                    Surface(color = Teal.copy(alpha = 0.1f), shape = RoundedCornerShape(100)) {
                        Text("₹500 DBT ✓",
                            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall, color = Teal)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (patient.dotsDue) {
                    Button(
                        onClick = onRecordDots,
                        colors = ButtonDefaults.buttonColors(containerColor = Teal),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Check, null, tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text(s.tbDotsRecord, color = Color.White)
                    }
                }
                OutlinedButton(
                    onClick = onOpenCalendar,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(s.tbCalendar)
                }
            }
        }
    }
}

@Composable
private fun AdherenceRing(percent: Int) {
    Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { percent / 100f },
            modifier = Modifier.fillMaxSize(),
            color = when {
                percent >= 90 -> RiskGreen
                percent >= 70 -> RiskAmber
                else -> RiskRed
            },
            trackColor = Color(0xFFE0E0E0),
            strokeWidth = 5.dp
        )
        Text("$percent%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DOTSCalendarDialog(
    patient: TBPatientDisplay,
    dotsCalendar: Map<String, String>,
    onMarkDay: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val s = appStrings()
    val lang = LocalAppLanguage.current
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val locale = when (lang) { "kn" -> Locale("kn"); "en" -> Locale.ENGLISH; else -> Locale("hi") }
    val monthName = SimpleDateFormat("MMMM yyyy", locale).format(calendar.time)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Column {
                Text(s.tbCalendarTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(monthName, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        },
        text = {
            Column {
                // Day grid
                val rows = (1..daysInMonth).chunked(7)
                rows.forEach { week ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        week.forEach { day ->
                            val c = Calendar.getInstance().apply {
                                set(Calendar.DAY_OF_MONTH, day)
                            }
                            val dateStr = fmt.format(c.time)
                            val status = dotsCalendar[dateStr]
                            val isToday = day == Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                            DOTSDay(
                                day = day,
                                status = status,
                                isToday = isToday,
                                onClick = {
                                    if (c.time <= Date()) {
                                        val newStatus = if (status == "TAKEN") "MISSED" else "TAKEN"
                                        onMarkDay(dateStr, newStatus)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Fill remaining
                        repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(s.tbClose) }
        }
    )
}

@Composable
private fun DOTSDay(
    day: Int,
    status: String?,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (bg, textColor) = when (status) {
        "TAKEN"  -> RiskGreen to Color.White
        "MISSED" -> RiskRed to Color.White
        else     -> if (isToday) Saffron.copy(alpha = 0.2f) to Saffron else Color(0xFFF5F5F5) to TextSecondary
    }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            day.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
        )
    }
}


@Composable
private fun LocalDotsCard(rec: com.ashasaathi.data.repository.LocalDotsRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (rec.dotsTaken) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
        ),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape)
                    .background(if (rec.dotsTaken) RiskGreen.copy(alpha = 0.15f) else RiskRed.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("💊", style = MaterialTheme.typography.bodyLarge)
            }
            Column(Modifier.weight(1f)) {
                Text(rec.patientName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (rec.nikshayId.isNotBlank())
                    Text("Nikshay: ${rec.nikshayId}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                if (rec.sideEffects.isNotBlank())
                    Text("Side effects: ${rec.sideEffects}", style = MaterialTheme.typography.bodySmall, color = RiskRed)
            }
            Surface(
                color = if (rec.dotsTaken) RiskGreen.copy(alpha = 0.15f) else RiskRed.copy(alpha = 0.15f),
                shape = RoundedCornerShape(100)
            ) {
                Text(
                    if (rec.dotsTaken) "✓ Taken" else "✗ Missed",
                    Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (rec.dotsTaken) RiskGreen else RiskRed,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun DemoTBContent(modifier: Modifier = Modifier) {
    val demoPatients = listOf(
        Triple("Ramesh Yadav", "NK-2024-001234", "Day 45 / 180 · 88% adherence"),
        Triple("Sushma Devi", "NK-2024-005678", "Day 12 / 180 · 100% adherence"),
        Triple("Mohan Lal", "NK-2023-009012", "Day 120 / 180 · 76% adherence"),
    )
    LazyColumn(modifier = modifier, contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("3 TB patients · Sample data", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        items(demoPatients) { (name, nikshay, progress) ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("💊 DOTS Due", style = MaterialTheme.typography.labelSmall, color = RiskAmber)
                    }
                    Text("Nikshay: $nikshay", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text(progress, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }
    }
}

package com.ashasaathi.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ashasaathi.data.model.Household
import com.ashasaathi.data.model.Patient
import com.ashasaathi.data.model.Visit
import com.ashasaathi.ui.LocalAppLanguage
import com.ashasaathi.ui.components.*
import com.ashasaathi.ui.components.voice.VoiceFAB
import com.ashasaathi.ui.strings.appStrings
import com.ashasaathi.ui.navigation.Route
import com.ashasaathi.ui.theme.*
import com.ashasaathi.ui.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*

private enum class DetailDialog { NONE, VISITS, HIGH_RISK, VACCINES, DOTS, FAMILIES, PATIENTS, THIS_MONTH }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    vm: HomeViewModel = hiltViewModel()
) {
    val worker        by vm.worker.collectAsState()
    val patients      by vm.patients.collectAsState()
    val households    by vm.households.collectAsState()
    val monthVisits   by vm.monthVisits.collectAsState()
    val metrics       by vm.metrics.collectAsState()
    val loading       by vm.loading.collectAsState()
    val isOnline      by vm.isOnline.collectAsState()
    val totalRecords  by vm.totalRecords.collectAsState()

    var openDialog by remember { mutableStateOf(DetailDialog.NONE) }

    val lang = LocalAppLanguage.current
    val s    = appStrings()
    val today = remember(lang) {
        val locale = when (lang) { "kn" -> Locale("kn"); "en" -> Locale.ENGLISH; else -> Locale("hi") }
        SimpleDateFormat("d MMMM, EEEE", locale).format(Date())
    }
    val prioritized = remember(patients) {
        patients.sortedWith(compareBy {
            when (it.currentRiskLevel) { "RED" -> 0; "YELLOW" -> 1; else -> 2 }
        })
    }

    // Detail dialogs
    when (openDialog) {
        DetailDialog.VISITS    -> PatientListDialog(
            title   = s.homeMetricVisits,
            emoji   = "📋",
            patients = prioritized.filter { it.currentRiskLevel != "GREEN" || it.isPregnant || it.isChildUnder5 || it.hasTB },
            lang    = lang, onDismiss = { openDialog = DetailDialog.NONE },
            onPatientClick = { navController.navigate(Route.patientDetail(it)); openDialog = DetailDialog.NONE }
        )
        DetailDialog.HIGH_RISK -> PatientListDialog(
            title   = s.homeMetricHighRisk,
            emoji   = "⚠️",
            patients = patients.filter { it.currentRiskLevel == "RED" },
            lang    = lang, onDismiss = { openDialog = DetailDialog.NONE },
            onPatientClick = { navController.navigate(Route.patientDetail(it)); openDialog = DetailDialog.NONE }
        )
        DetailDialog.VACCINES  -> PatientListDialog(
            title   = when (lang) { "kn" -> "ಲಸಿಕೆ ಬಾಕಿ"; "en" -> "Vaccines Due"; else -> "टीके बाकी" },
            emoji   = "💉",
            patients = patients.filter { it.isChildUnder5 && !it.ficStatus },
            lang    = lang, onDismiss = { openDialog = DetailDialog.NONE },
            onPatientClick = { navController.navigate(Route.patientDetail(it)); openDialog = DetailDialog.NONE }
        )
        DetailDialog.DOTS      -> PatientListDialog(
            title   = when (lang) { "kn" -> "TB DOTS ರೋಗಿಗಳು"; "en" -> "TB DOTS Patients"; else -> "TB DOTS मरीज़" },
            emoji   = "💊",
            patients = patients.filter { it.hasTB },
            lang    = lang, onDismiss = { openDialog = DetailDialog.NONE },
            onPatientClick = { navController.navigate(Route.patientDetail(it)); openDialog = DetailDialog.NONE }
        )
        DetailDialog.FAMILIES  -> HouseholdListDialog(
            households = households,
            lang = lang,
            onDismiss  = { openDialog = DetailDialog.NONE }
        )
        DetailDialog.PATIENTS  -> PatientListDialog(
            title   = when (lang) { "kn" -> "ಎಲ್ಲ ರೋಗಿಗಳು"; "en" -> "All Patients"; else -> "सभी मरीज़" },
            emoji   = "👤",
            patients = patients,
            lang    = lang, onDismiss = { openDialog = DetailDialog.NONE },
            onPatientClick = { navController.navigate(Route.patientDetail(it)); openDialog = DetailDialog.NONE }
        )
        DetailDialog.THIS_MONTH -> VisitListDialog(
            visits  = monthVisits,
            patients= patients,
            lang    = lang,
            onDismiss = { openDialog = DetailDialog.NONE }
        )
        DetailDialog.NONE -> Unit
    }

    Scaffold(
        containerColor = WarmBackground,
        bottomBar = { AppBottomBar(navController) },
        floatingActionButton = {
            VoiceFAB(onExtracted = {})
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {

            // ── Header ─────────────────────────────────────────────────────────
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(SaffronGlow, Saffron, SaffronDark),
                                start = Offset(0f, 0f),
                                end   = Offset(Float.POSITIVE_INFINITY, 500f)
                            )
                        )
                        .padding(horizontal = 20.dp)
                        .padding(top = 56.dp, bottom = 36.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            // App name — prominent
                            Text(
                                when (lang) {
                                    "kn" -> "ಆಶಾ ಸಾಥಿ"; "en" -> "AshaSaathi"; else -> "आशा साथी"
                                },
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                when (lang) {
                                    "kn" -> "ನಮಸ್ಕಾರ 🙏"
                                    "en" -> "Hello 🙏"
                                    else -> "नमस्ते 🙏"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Text(
                                worker?.name?.substringBefore(" ") ?: when (lang) {
                                    "kn" -> "ಸ್ನೇಹಿತ"; "en" -> "Friend"; else -> "साथी"
                                },
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(Modifier.height(4.dp))
                            // Date pill
                            Surface(
                                shape = RoundedCornerShape(100),
                                color = Color.White.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    today,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                            }
                            if (!isOnline) {
                                Spacer(Modifier.height(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(100),
                                    color = Color(0xFFFFEB3B).copy(alpha = 0.22f)
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFFFEB3B)))
                                        Text(
                                            s.offline,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFFFEB3B),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        // Avatar ring
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                Modifier
                                    .size(58.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.25f))
                            )
                            Box(
                                Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.20f))
                                    .clickable { navController.navigate(Route.SETTINGS) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    (worker?.name ?: "A").first().uppercase(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }

            // ── Metric cards ───────────────────────────────────────────────────
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Spacer(Modifier.height((-20).dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = Saffron.copy(alpha = 0.12f)),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        if (loading) {
                            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                repeat(4) { CardSkeleton(Modifier.weight(1f).height(100.dp)) }
                            }
                        } else {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    MetricCard("📋", s.homeMetricVisits,   metrics.planned.toString(),  Saffron,   Modifier.weight(1f).clickable { openDialog = DetailDialog.VISITS })
                                    MetricCard("⚠️", s.homeMetricHighRisk, metrics.highRisk.toString(), RiskRed,   Modifier.weight(1f).clickable { openDialog = DetailDialog.HIGH_RISK })
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    MetricCard("💉", s.homeMetricVaccines, metrics.vaccines.toString(), Teal,      Modifier.weight(1f).clickable { openDialog = DetailDialog.VACCINES })
                                    MetricCard("💊", s.homeMetricDots,     metrics.dots.toString(),     RiskAmber, Modifier.weight(1f).clickable { openDialog = DetailDialog.DOTS })
                                }
                            }
                        }
                    }
                }
            }

            // ── Stats strip ────────────────────────────────────────────────────
            item {
                Row(
                    Modifier.padding(horizontal = 16.dp).padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RecordChip("🏠", totalRecords.households.toString(), s.homeTotalHouseholds, Teal,     Modifier.weight(1f).clickable { openDialog = DetailDialog.FAMILIES })
                    RecordChip("👤", totalRecords.patients.toString(),   s.homeTotalPatients,   Saffron,  Modifier.weight(1f).clickable { openDialog = DetailDialog.PATIENTS })
                    RecordChip("📝", totalRecords.visitsThisMonth.toString(), s.homeTotalVisitsMonth, RiskGreen, Modifier.weight(1f).clickable { openDialog = DetailDialog.THIS_MONTH })
                }
            }

            // ── High-risk alert ────────────────────────────────────────────────
            if (!loading && prioritized.any { it.currentRiskLevel == "RED" }) {
                item {
                    val redCount = prioritized.count { it.currentRiskLevel == "RED" }
                    Card(
                        modifier = Modifier.padding(horizontal = 16.dp).padding(top = 10.dp).fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = RiskRedSurface),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, RiskRed.copy(alpha = 0.22f))
                    ) {
                        Row(
                            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Warning, null, tint = RiskRed, modifier = Modifier.size(18.dp))
                            Text(
                                s.homeHighRiskAlert.format(redCount),
                                style = MaterialTheme.typography.labelMedium,
                                color = RiskRed,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // ── Voice entry card ───────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                        .clickable { navController.navigate(Route.VOICE_FORM) },
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Box(
                        Modifier
                            .background(
                                Brush.linearGradient(
                                    listOf(SaffronGlow, Saffron, SaffronDark),
                                    start = Offset(0f, 0f),
                                    end   = Offset(Float.POSITIVE_INFINITY, 100f)
                                )
                            )
                            .padding(horizontal = 20.dp, vertical = 18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.20f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Mic, null, tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                            Column(Modifier.weight(1f)) {
                                Text(s.homeVoiceLabel,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White)
                                Text(s.homeVoiceSub,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.80f))
                            }
                            Icon(Icons.Default.ArrowForward, null, tint = Color.White.copy(alpha = 0.80f), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // ── Workplan ───────────────────────────────────────────────────────
            item { SectionHeader(s.homeWorkplanTitle) }

            if (loading) {
                items(4) { CardSkeleton(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
            } else if (prioritized.isEmpty()) {
                items(demoWorkplanTasks) { task -> DemoTaskCard(task) }
            } else {
                itemsIndexed(prioritized.take(30), key = { _, p -> p.patientId }) { _, patient ->
                    WorkplanCard(
                        patient = patient,
                        onClick = { navController.navigate(Route.patientDetail(patient.patientId)) }
                    )
                }
            }

            // ── Quick actions ──────────────────────────────────────────────────
            item { SectionHeader(s.homeQuickActions) }
            item {
                Column(
                    Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickActionButton(s.homeAddFamily,     Icons.Default.House,          Teal,      Modifier.weight(1f)) { navController.navigate(Route.ADD_HOUSEHOLD) }
                        QuickActionButton(s.homeViewMap,       Icons.Default.Map,            Saffron,   Modifier.weight(1f)) { navController.navigate(Route.MAP) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickActionButton(s.homeMonthlyReport, Icons.Default.BarChart,       RiskAmber, Modifier.weight(1f)) { navController.navigate(Route.REPORTS) }
                        QuickActionButton(s.homeDotsTracker,   Icons.Default.MedicalServices,RiskRed,   Modifier.weight(1f)) { navController.navigate(Route.TB_DOTS) }
                    }
                }
            }
        }
    }
}

// ── Record stat chip ──────────────────────────────────────────────────────────

@Composable
private fun RecordChip(emoji: String, count: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.20f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            Modifier.padding(horizontal = 10.dp, vertical = 10.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(emoji, fontSize = 16.sp)
            Text(count, style = MaterialTheme.typography.titleSmall, color = color, fontWeight = FontWeight.ExtraBold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

// ── Workplan card ─────────────────────────────────────────────────────────────

@Composable
fun WorkplanCard(patient: Patient, onClick: () -> Unit) {
    val s = appStrings()
    val riskColor = when (patient.currentRiskLevel) {
        "RED" -> RiskRed; "YELLOW" -> RiskAmber; else -> RiskGreen
    }
    val riskBg = when (patient.currentRiskLevel) {
        "RED" -> RiskRedSurface.copy(alpha = 0.5f); "YELLOW" -> RiskAmberSurface.copy(alpha = 0.5f); else -> Color.White
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = riskBg),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, riskColor.copy(alpha = 0.15f))
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(listOf(riskColor, riskColor.copy(alpha = 0.5f)))
                    )
                    .clip(RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp))
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(patient.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        patient.village?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                    RiskBadge(patient.currentRiskLevel, Modifier.padding(start = 8.dp))
                }
                if (patient.isPregnant || patient.isChildUnder5 || patient.hasTB || patient.isElderly) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (patient.isPregnant)    ColorChip(s.tagPregnant,  RiskAmber)
                        if (patient.isChildUnder5) ColorChip(s.tagChild,     Teal)
                        if (patient.hasTB)         ColorChip("💊 TB-DOTS",   RiskRed)
                        if (patient.isElderly)     ColorChip(s.tagElderly,   TextSecondary)
                    }
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = TextHint,
                modifier = Modifier.align(Alignment.CenterVertically).padding(end = 10.dp)
            )
        }
    }
}

// ── Quick action button ───────────────────────────────────────────────────────

@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.20f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            }
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 17.sp
            )
        }
    }
}

// ── Demo tasks ────────────────────────────────────────────────────────────────

data class DemoTask(val name: String, val tag: String, val tagColor: Color, val riskColor: Color, val village: String)

val demoWorkplanTasks = listOf(
    DemoTask("Sunita Devi",      "🤰 Pregnant",   Color(0xFFF59E0B), Color(0xFFF59E0B), "Rampur"),
    DemoTask("Ravi Kumar (Child)","💉 Vaccine Due",Color(0xFF0D9488), Color(0xFF0D9488), "Sarkari Tola"),
    DemoTask("Meena Bai",        "💊 TB-DOTS",    Color(0xFFEF4444), Color(0xFFEF4444), "Nayagaon"),
    DemoTask("Kamla Devi",       "👴 Elderly",    Color(0xFF64748B), Color(0xFF22C55E), "Pipra"),
    DemoTask("Geeta Singh",      "🤰 Pregnant",   Color(0xFFF59E0B), Color(0xFFEF4444), "Basahi"),
)

@Composable
fun DemoTaskCard(task: DemoTask) {
    val riskBg = when (task.riskColor) {
        Color(0xFFEF4444) -> RiskRedSurface.copy(alpha = 0.5f)
        Color(0xFFF59E0B) -> RiskAmberSurface.copy(alpha = 0.5f)
        else              -> Color.White
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = riskBg),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, task.riskColor.copy(alpha = 0.15f))
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(Brush.verticalGradient(listOf(task.riskColor, task.riskColor.copy(alpha = 0.4f))))
                    .clip(RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp))
            )
            Column(Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(task.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text(task.village, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Surface(
                        color = task.riskColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(100),
                        border = BorderStroke(0.5.dp, task.riskColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            "Sample",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = task.riskColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Surface(
                    color = task.tagColor.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(100),
                    border = BorderStroke(0.5.dp, task.tagColor.copy(alpha = 0.30f))
                ) {
                    Text(
                        task.tag,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = task.tagColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ── Detail dialogs ────────────────────────────────────────────────────────────

@Composable
private fun DetailDialogShell(
    title: String,
    emoji: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.78f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(Modifier.fillMaxSize()) {
                // Header
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(listOf(SaffronGlow, SaffronDark),
                                start = Offset(0f, 0f), end = Offset(Float.POSITIVE_INFINITY, 0f))
                        )
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(emoji, fontSize = 22.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(title,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.20f))
                        ) {
                            Icon(Icons.Default.Close, "Close", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                // Scrollable content
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
private fun PatientListDialog(
    title: String,
    emoji: String,
    patients: List<Patient>,
    lang: String,
    onDismiss: () -> Unit,
    onPatientClick: (String) -> Unit
) {
    DetailDialogShell(title = title, emoji = emoji, onDismiss = onDismiss) {
        if (patients.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    when (lang) { "en" -> "No records found"; "kn" -> "ದಾಖಲೆಗಳಿಲ್ಲ"; else -> "कोई रिकॉर्ड नहीं" },
                    style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center
                )
            }
        } else {
            patients.forEach { p ->
                val riskColor = when (p.currentRiskLevel) { "RED" -> RiskRed; "YELLOW" -> RiskAmber; else -> RiskGreen }
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onPatientClick(p.patientId) },
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, riskColor.copy(alpha = 0.18f)),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        Modifier.height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.width(4.dp).fillMaxHeight()
                            .background(Brush.verticalGradient(listOf(riskColor, riskColor.copy(0.4f))))
                            .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)))
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(p.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                RiskBadge(p.currentRiskLevel)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                p.village?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = TextSecondary) }
                                p.age?.let { Text("${it}y", style = MaterialTheme.typography.bodySmall, color = TextSecondary) }
                            }
                            val tags = buildList {
                                if (p.isPregnant)    add("🤰")
                                if (p.isChildUnder5) add("👶")
                                if (p.hasTB)         add("💊 TB")
                                if (p.isElderly)     add("👴")
                            }
                            if (tags.isNotEmpty()) {
                                Text(tags.joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = TextHint, modifier = Modifier.padding(end = 8.dp).size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun HouseholdListDialog(households: List<Household>, lang: String, onDismiss: () -> Unit) {
    val title = when (lang) { "kn" -> "ಕುಟುಂಬಗಳು"; "en" -> "Families"; else -> "परिवार" }
    DetailDialogShell(title = title, emoji = "🏠", onDismiss = onDismiss) {
        if (households.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    when (lang) { "en" -> "No households registered"; "kn" -> "ಯಾವ ಕುಟುಂಬವಿಲ್ಲ"; else -> "कोई परिवार नहीं" },
                    style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center
                )
            }
        } else {
            households.forEach { h ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Teal.copy(alpha = 0.15f)),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Teal.copy(0.10f)),
                            contentAlignment = Alignment.Center
                        ) { Text("🏠", fontSize = 18.sp) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(h.headOfFamily.ifBlank { "—" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text("${when(lang){"kn"->"ಮನೆ";"en"->"House";else->"घर"}} #${h.houseNumber} · ${h.village.ifBlank{"—"}}",
                                style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Surface(color = Teal.copy(0.10f), shape = RoundedCornerShape(100), border = BorderStroke(0.5.dp, Teal.copy(0.25f))) {
                            Text("${h.totalMembers} ${when(lang){"kn"->"ಜನ";"en"->"members";else->"सदस्य"}}",
                                Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall, color = Teal, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VisitListDialog(visits: List<Visit>, patients: List<Patient>, lang: String, onDismiss: () -> Unit) {
    val title = when (lang) { "kn" -> "ಈ ತಿಂಗಳ ಭೇಟಿಗಳು"; "en" -> "Visits This Month"; else -> "इस महीने की विजिट" }
    val patientMap = remember(patients) { patients.associateBy { it.patientId } }
    DetailDialogShell(title = title, emoji = "📝", onDismiss = onDismiss) {
        if (visits.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    when (lang) { "en" -> "No visits this month"; "kn" -> "ಈ ತಿಂಗಳ ಭೇಟಿಗಳಿಲ್ಲ"; else -> "इस महीने कोई विजिट नहीं" },
                    style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center
                )
            }
        } else {
            visits.sortedByDescending { it.visitDate }.forEach { v ->
                val patient = patientMap[v.patientId]
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, RiskGreen.copy(alpha = 0.15f)),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(RiskGreen.copy(0.10f)),
                            contentAlignment = Alignment.Center
                        ) { Text("📋", fontSize = 18.sp) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(patient?.name ?: "—", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(v.visitDate, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Text("·", style = MaterialTheme.typography.bodySmall, color = TextHint)
                                Text(v.visitType, style = MaterialTheme.typography.bodySmall, color = Teal)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Bottom nav ────────────────────────────────────────────────────────────────

data class NavItem(val route: String, val icon: ImageVector, val labelHi: String)

@Composable
fun AppBottomBar(navController: NavController) {
    val current = navController.currentBackStackEntryAsState()
    val route   = current.value?.destination?.route
    val lang    = LocalAppLanguage.current

    val items = when (lang) {
        "kn" -> listOf(
            NavItem(Route.HOME,        Icons.Default.Home,            "ಹೋಮ್"),
            NavItem(Route.HOUSEHOLDS,  Icons.Default.House,           "ಕುಟುಂಬ"),
            NavItem(Route.VACCINATION, Icons.Default.MedicalServices, "ಲಸಿಕೆ"),
            NavItem(Route.PLANNER,     Icons.Default.DateRange,       "ಯೋಜನೆ"),
            NavItem(Route.SETTINGS,    Icons.Default.Settings,        "ಸೆಟ್ಟಿಂಗ್"),
        )
        "en" -> listOf(
            NavItem(Route.HOME,        Icons.Default.Home,            "Home"),
            NavItem(Route.HOUSEHOLDS,  Icons.Default.House,           "Families"),
            NavItem(Route.VACCINATION, Icons.Default.MedicalServices, "Vaccines"),
            NavItem(Route.PLANNER,     Icons.Default.DateRange,       "Planner"),
            NavItem(Route.SETTINGS,    Icons.Default.Settings,        "Settings"),
        )
        else -> listOf(
            NavItem(Route.HOME,        Icons.Default.Home,            "होम"),
            NavItem(Route.HOUSEHOLDS,  Icons.Default.House,           "परिवार"),
            NavItem(Route.VACCINATION, Icons.Default.MedicalServices, "टीके"),
            NavItem(Route.PLANNER,     Icons.Default.DateRange,       "प्लानर"),
            NavItem(Route.SETTINGS,    Icons.Default.Settings,        "सेटिंग"),
        )
    }

    // Floating pill — custom Row so we own the insets and corner padding
    Box(
        Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(18.dp, RoundedCornerShape(28.dp), spotColor = Color.Black.copy(alpha = 0.08f)),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            // 8dp horizontal inset keeps corner items clear of the 28dp curve
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val selected = route == item.route
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                indication = null,
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            ) {
                                navController.navigate(item.route) {
                                    launchSingleTop = true
                                    restoreState    = true
                                    popUpTo(Route.HOME) { saveState = true }
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        // Pill indicator behind icon when selected
                        Box(
                            Modifier
                                .size(width = 46.dp, height = 30.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(if (selected) SaffronContainer else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                item.icon,
                                contentDescription = item.labelHi,
                                modifier = Modifier.size(20.dp),
                                tint = if (selected) Saffron else TextSecondary
                            )
                        }
                        Text(
                            item.labelHi,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) Saffron else TextSecondary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

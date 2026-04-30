package com.ashasaathi.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ashasaathi.data.model.Patient
import com.ashasaathi.ui.LocalAppLanguage
import com.ashasaathi.ui.components.*
import com.ashasaathi.ui.components.voice.VoiceFAB
import com.ashasaathi.ui.navigation.Route
import com.ashasaathi.ui.theme.*
import com.ashasaathi.ui.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    vm: HomeViewModel = hiltViewModel()
) {
    val worker        by vm.worker.collectAsState()
    val patients      by vm.patients.collectAsState()
    val metrics       by vm.metrics.collectAsState()
    val loading       by vm.loading.collectAsState()
    val isOnline      by vm.isOnline.collectAsState()
    val totalRecords  by vm.totalRecords.collectAsState()

    val lang = LocalAppLanguage.current
    val today = remember(lang) {
        val locale = when (lang) { "kn" -> Locale("kn"); "en" -> Locale.ENGLISH; else -> Locale("hi") }
        SimpleDateFormat("d MMMM yyyy, EEEE", locale).format(Date())
    }
    val prioritized = remember(patients) {
        patients.sortedWith(compareBy {
            when (it.currentRiskLevel) { "RED" -> 0; "YELLOW" -> 1; else -> 2 }
        })
    }

    Scaffold(
        containerColor = WarmBackground,
        bottomBar = { AppBottomBar(navController) },
        floatingActionButton = {
            VoiceFAB(onExtracted = { /* global voice capture: navigate to visit form */ })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {

            // ── Header ─────────────────────────────────────────────────────────
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Saffron, SaffronDark)))
                        .padding(horizontal = 20.dp)
                        .padding(top = 52.dp, bottom = 28.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                when (lang) {
                                    "kn" -> "ನಮಸ್ಕಾರ, ${worker?.name?.substringBefore(" ") ?: "ಸ್ನೇಹಿತ"} 🙏"
                                    "en" -> "Hello, ${worker?.name?.substringBefore(" ") ?: "Friend"} 🙏"
                                    else -> "नमस्ते, ${worker?.name?.substringBefore(" ") ?: "साथी"} 🙏"
                                },
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                today,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            if (!isOnline) {
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFFEB3B)))
                                    Text("ऑफलाइन — डेटा सेव है",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFFFEB3B))
                                }
                            }
                        }
                        // Avatar
                        Box(
                            Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                                .clickable { navController.navigate(Route.SETTINGS) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                (worker?.name ?: "A").first().uppercase(),
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // ── Metric cards ───────────────────────────────────────────────────
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Spacer(Modifier.height((-16).dp))  // overlap header
                    Card(
                        modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        if (loading) {
                            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                repeat(4) { CardSkeleton(Modifier.weight(1f).height(90.dp)) }
                            }
                        } else {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    MetricCard("📋", "आज की\nविजिट", metrics.planned.toString(), Saffron, Modifier.weight(1f))
                                    MetricCard("⚠️", "उच्च\nजोखिम", metrics.highRisk.toString(), RiskRed, Modifier.weight(1f))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    MetricCard("💉", "टीके\nबाकी", metrics.vaccines.toString(), Teal, Modifier.weight(1f))
                                    MetricCard("💊", "DOTS\nबाकी", metrics.dots.toString(), RiskAmber, Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // ── Total records summary ──────────────────────────────────────────
            item {
                Row(
                    Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RecordChip("🏠 ${totalRecords.households}", "परिवार", Teal, Modifier.weight(1f))
                    RecordChip("👤 ${totalRecords.patients}", "मरीज़", Saffron, Modifier.weight(1f))
                    RecordChip("📝 ${totalRecords.visitsThisMonth}", "इस माह", RiskGreen, Modifier.weight(1f))
                }
            }

            // ── Reminder chips ─────────────────────────────────────────────────
            if (!loading && prioritized.any { it.currentRiskLevel == "RED" }) {
                item {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val redCount = prioritized.count { it.currentRiskLevel == "RED" }
                        Surface(
                            color = RiskRedSurface,
                            shape = RoundedCornerShape(100)
                        ) {
                            Text(
                                "⚠️ $redCount उच्च जोखिम — पहले मिलें",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = RiskRed,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // ── Workplan ───────────────────────────────────────────────────────
            item {
                SectionHeader("आज का कार्यक्रम")
            }

            if (loading) {
                items(4) {
                    CardSkeleton(Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                }
            } else if (prioritized.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "🌟",
                        titleHi = "आज सब ठीक है!",
                        subtitleEn = "No urgent visits today. Great work!",
                        modifier = Modifier.padding(top = 24.dp)
                    )
                }
            } else {
                itemsIndexed(prioritized.take(30), key = { _, p -> p.patientId }) { _, patient ->
                    WorkplanCard(
                        patient = patient,
                        onClick = { navController.navigate(Route.patientDetail(patient.patientId)) }
                    )
                }
            }

            // ── Voice Entry ────────────────────────────────────────────────────
            item {
                val voiceLabel = when (lang) {
                    "kn" -> "🎤  ಧ್ವನಿಯಿಂದ ದಾಖಲು ಮಾಡಿ"
                    "en" -> "🎤  Voice Entry"
                    else -> "🎤  आवाज़ से दर्ज करें"
                }
                val voiceSub = when (lang) {
                    "kn" -> "ಮನೆ · ರೋಗಿ · ANC · ಲಸಿಕೆ · DOTS"
                    "en" -> "Household · Patient · ANC · Vaccine · DOTS"
                    else -> "परिवार · मरीज़ · ANC · टीका · DOTS"
                }
                Card(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp, bottom = 4.dp)
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(16.dp))
                        .clickable { navController.navigate(Route.VOICE_FORM) },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .background(Brush.horizontalGradient(listOf(Saffron, SaffronDark)))
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(voiceLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White)
                            Text(voiceSub,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f))
                        }
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // ── Quick actions ──────────────────────────────────────────────────
            item {
                SectionHeader("त्वरित कार्य")
            }
            item {
                Column(
                    Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickActionButton("🏠\nपरिवार जोड़ें", Teal, Modifier.weight(1f)) {
                            navController.navigate(Route.ADD_HOUSEHOLD)
                        }
                        QuickActionButton("🗺️\nनक्शा देखें", Saffron, Modifier.weight(1f)) {
                            navController.navigate(Route.MAP)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickActionButton("📋\nमासिक रिपोर्ट", RiskAmber, Modifier.weight(1f)) {
                            navController.navigate(Route.REPORTS)
                        }
                        QuickActionButton("💊\nDOTS ट्रैकर", RiskRed, Modifier.weight(1f)) {
                            navController.navigate(Route.TB_DOTS)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordChip(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleSmall, color = color, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

@Composable
fun WorkplanCard(patient: Patient, onClick: () -> Unit) {
    val riskColor = when (patient.currentRiskLevel) {
        "RED" -> RiskRed; "YELLOW" -> RiskAmber; else -> RiskGreen
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(riskColor)
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
                        Text(patient.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        patient.village?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                    RiskBadge(patient.currentRiskLevel, Modifier.padding(start = 8.dp))
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (patient.isPregnant)    ColorChip("🤰 गर्भवती",  RiskAmber)
                    if (patient.isChildUnder5) ColorChip("👶 शिशु",     Teal)
                    if (patient.hasTB)         ColorChip("💊 TB-DOTS",   RiskRed)
                    if (patient.isElderly)     ColorChip("👴 वृद्ध",     TextSecondary)
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = TextHint,
                modifier = Modifier.align(Alignment.CenterVertically).padding(end = 8.dp)
            )
        }
    }
}

@Composable
private fun QuickActionButton(label: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Text(
            label,
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 18.sp
        )
    }
}

// ── Bottom nav ────────────────────────────────────────────────────────────────

data class NavItem(val route: String, val icon: ImageVector, val labelHi: String)

@Composable
fun AppBottomBar(navController: NavController) {
    val current  by navController.currentBackStackEntryAsState()
    val route    = current?.destination?.route
    val lang     = LocalAppLanguage.current

    val items = when (lang) {
        "kn" -> listOf(
            NavItem(Route.HOME,       Icons.Default.Home,            "ಹೋಮ್"),
            NavItem(Route.HOUSEHOLDS, Icons.Default.House,           "ಕುಟುಂಬ"),
            NavItem(Route.VACCINATION,Icons.Default.MedicalServices, "ಲಸಿಕೆ"),
            NavItem(Route.PLANNER,    Icons.Default.DateRange,       "ಯೋಜನೆ"),
            NavItem(Route.SETTINGS,   Icons.Default.Settings,        "ಸೆಟ್ಟಿಂಗ್"),
        )
        "en" -> listOf(
            NavItem(Route.HOME,       Icons.Default.Home,            "Home"),
            NavItem(Route.HOUSEHOLDS, Icons.Default.House,           "Families"),
            NavItem(Route.VACCINATION,Icons.Default.MedicalServices, "Vaccines"),
            NavItem(Route.PLANNER,    Icons.Default.DateRange,       "Planner"),
            NavItem(Route.SETTINGS,   Icons.Default.Settings,        "Settings"),
        )
        else -> listOf(
            NavItem(Route.HOME,       Icons.Default.Home,            "होम"),
            NavItem(Route.HOUSEHOLDS, Icons.Default.House,           "परिवार"),
            NavItem(Route.VACCINATION,Icons.Default.MedicalServices, "टीके"),
            NavItem(Route.PLANNER,    Icons.Default.DateRange,       "प्लानर"),
            NavItem(Route.SETTINGS,   Icons.Default.Settings,        "सेटिंग"),
        )
    }

    NavigationBar(
        containerColor = Color.White,
        modifier = Modifier.shadow(12.dp)
    ) {
        items.forEach { item ->
            NavigationBarItem(
                icon    = { Icon(item.icon, item.labelHi, modifier = Modifier.size(26.dp)) },
                label   = { Text(item.labelHi, style = MaterialTheme.typography.labelSmall) },
                selected = route == item.route,
                onClick  = {
                    navController.navigate(item.route) {
                        launchSingleTop = true
                        restoreState    = true
                        popUpTo(Route.HOME) { saveState = true }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = Saffron,
                    selectedTextColor   = Saffron,
                    indicatorColor      = SaffronContainer,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                )
            )
        }
    }
}

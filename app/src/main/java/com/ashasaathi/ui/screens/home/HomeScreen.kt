package com.ashasaathi.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ashasaathi.data.model.Patient
import com.ashasaathi.ui.navigation.Route
import com.ashasaathi.ui.theme.*
import com.ashasaathi.ui.viewmodel.HomeViewModel
import com.ashasaathi.ui.components.voice.VoiceFAB
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    vm: HomeViewModel = hiltViewModel()
) {
    val worker by vm.worker.collectAsState()
    val patients by vm.patients.collectAsState()
    val metrics by vm.metrics.collectAsState()
    val isOnline by vm.isOnline.collectAsState()

    val today = remember { SimpleDateFormat("d MMMM yyyy", Locale("hi")).format(Date()) }
    val prioritized = remember(patients) {
        patients.sortedWith(compareBy {
            when (it.currentRiskLevel) { "RED" -> 0; "YELLOW" -> 1; else -> 2 }
        })
    }

    Scaffold(
        bottomBar = { AppBottomBar(navController) },
        floatingActionButton = { VoiceFAB() }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                // Header
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(Primary)
                        .padding(horizontal = 16.dp)
                        .padding(top = 52.dp, bottom = 24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "नमस्ते, ${worker?.name ?: ""}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "आज: $today",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                (worker?.name ?: "A").first().toString(),
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            if (!isOnline) item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(RiskAmber)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⚡ ऑफलाइन • Offline mode", color = Color.White, style = MaterialTheme.typography.bodySmall)
                }
            }

            item {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard("📋", "नियोजित\nविजिट", metrics.planned.toString(), Secondary, Modifier.weight(1f))
                    MetricCard("⚠️", "उच्च\nजोखिम", metrics.highRisk.toString(), RiskRed, Modifier.weight(1f))
                }
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard("💉", "टीके\nबाकी", metrics.vaccines.toString(), RiskAmber, Modifier.weight(1f))
                    MetricCard("💊", "DOTS\nबाकी", metrics.dots.toString(), Primary, Modifier.weight(1f))
                }
            }

            item {
                Text(
                    "मेरी कार्यसूची",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            if (prioritized.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            Modifier.padding(32.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🌟", fontSize = 52.sp)
                            Text("आज कोई विजिट नहीं", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
                            Text("No visits planned today", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }
            } else {
                itemsIndexed(prioritized.take(20)) { _, patient ->
                    WorkplanCard(patient = patient, onClick = {
                        navController.navigate(Route.patientDetail(patient.patientId))
                    })
                }
            }
        }
    }
}

@Composable
fun MetricCard(icon: String, label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(icon, fontSize = 28.sp)
            Text(value, style = MaterialTheme.typography.headlineSmall, color = color, fontWeight = FontWeight.Bold)
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
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(riskColor))
            Column(Modifier.padding(12.dp).weight(1f)) {
                Text(patient.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (patient.isPregnant) RiskChip("🤰 गर्भवती", RiskAmber)
                    if (patient.isChildUnder5) RiskChip("👶 शिशु", Color(0xFF7B1FA2))
                    if (patient.hasTB) RiskChip("💊 TB", RiskRed)
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = TextSecondary, modifier = Modifier.align(Alignment.CenterVertically).padding(end = 8.dp))
        }
    }
}

@Composable
fun RiskChip(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(100),
        color = color.copy(alpha = 0.1f)
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
fun AppBottomBar(navController: NavController) {
    val items = listOf(
        Triple(Route.HOME, Icons.Default.Home, "Home"),
        Triple(Route.HOUSEHOLDS, Icons.Default.House, "Households"),
        Triple(Route.VACCINATION, Icons.Default.Vaccines, "Vaccines"),
        Triple(Route.PLANNER, Icons.Default.CalendarMonth, "Planner"),
        Triple(Route.SETTINGS, Icons.Default.Settings, "Settings"),
    )
    NavigationBar(containerColor = Color.White) {
        items.forEach { (route, icon, label) ->
            NavigationBarItem(
                icon = { Icon(icon, label) },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                selected = false,
                onClick = { navController.navigate(route) { launchSingleTop = true } }
            )
        }
    }
}

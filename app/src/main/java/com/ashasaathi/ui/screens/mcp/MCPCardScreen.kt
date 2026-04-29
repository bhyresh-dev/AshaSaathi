package com.ashasaathi.ui.screens.mcp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ashasaathi.ui.components.InfoRow
import com.ashasaathi.ui.components.SectionHeader
import com.ashasaathi.ui.theme.*
import com.ashasaathi.ui.viewmodel.MCPCardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MCPCardScreen(
    navController: NavController,
    vm: MCPCardViewModel = hiltViewModel()
) {
    val patient by vm.patient.collectAsState()
    val visits  by vm.ancVisits.collectAsState()

    Scaffold(
        containerColor = WarmBackground,
        topBar = {
            TopAppBar(
                title = { Text("MCP कार्ड", color = Color.White) },
                navigationIcon = {
                    IconButton({ navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    IconButton({}) { Icon(Icons.Default.Print, null, tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Saffron)
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card Header
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Saffron, SaffronDark)),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("🏥 मातृ एवं शिशु सुरक्षा कार्ड", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center)
                    Text("Mother & Child Protection Card (RCH-II)", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text("National Health Mission — Government of India", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, textAlign = TextAlign.Center)
                }
            }

            patient?.let { p ->
                // Mother details
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SectionHeader("माँ की जानकारी / Mother Details")
                        InfoRow("नाम / Name", p.name)
                        InfoRow("उम्र / Age", "${p.age ?: "–"} वर्ष")
                        p.rchMctsId?.let     { InfoRow("RCH/MCTS ID", it) }
                        p.phone?.let         { InfoRow("फोन / Phone", it) }
                        p.village?.let       { InfoRow("गाँव / Village", it) }
                        p.bloodGroup?.let    { InfoRow("Blood Group", it) }
                        p.lmpDate?.let       { InfoRow("LMP तारीख", it) }
                        p.edd?.let           { InfoRow("Expected Delivery (EDD)", it) }
                        p.gestationalAgeWeeks?.let { InfoRow("Gestational Age", "$it weeks") }
                        InfoRow("ANC Count", p.ancCount.toString())
                    }
                }

                // IFA + TT
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionHeader("IFA / TT प्रगति")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("IFA गोलियाँ (180 में से)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Spacer(Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { (p.ifaCumulativeCount ?: 0) / 180f },
                                    modifier = Modifier.fillMaxWidth().height(10.dp),
                                    color = Teal,
                                    trackColor = TealContainer
                                )
                                Text("${p.ifaCumulativeCount ?: 0} / 180", style = MaterialTheme.typography.labelSmall, color = TextHint)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("TT Dose", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    when (p.ttDoseStatus) {
                                        "TT1" -> "TT1 ✓"; "TT2" -> "TT2 ✓✓"; "BOOSTER" -> "Booster ✓"; else -> "–"
                                    },
                                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Teal
                                )
                            }
                        }
                    }
                }

                // ANC Visit Table
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        SectionHeader("ANC विजिट रिकॉर्ड")
                        Spacer(Modifier.height(8.dp))

                        // Header row
                        Row(
                            Modifier.fillMaxWidth().background(SaffronContainer, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            listOf("विजिट", "तारीख", "BP", "Hb", "जोखिम").forEachIndexed { i, h ->
                                Text(h, Modifier.weight(if (i == 0) 0.6f else 1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = SaffronDark)
                            }
                        }
                        if (visits.isEmpty()) {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                Text("कोई ANC विजिट दर्ज नहीं", style = MaterialTheme.typography.bodySmall, color = TextHint)
                            }
                        } else {
                            visits.take(10).forEachIndexed { i, v ->
                                val isEven = i % 2 == 0
                                Row(
                                    Modifier.fillMaxWidth().background(if (isEven) Color.White else WarmBackground).padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("ANC ${i + 1}", Modifier.weight(0.6f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    Text(v.visitDate.take(10), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                    val bp = if (v.vitals.bpSystolic != null) "${v.vitals.bpSystolic}/${v.vitals.bpDiastolic}" else "–"
                                    Text(bp, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                    Text(v.vitals.hemoglobinGdL?.toString() ?: "–", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                    val riskColor = when (v.riskLevel) { "RED" -> RiskRed; "YELLOW" -> RiskAmber; else -> RiskGreen }
                                    Text(v.riskLevel, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = riskColor, fontWeight = FontWeight.Bold)
                                }
                                Divider(color = Color(0xFFEEEEEE))
                            }
                        }
                    }
                }

                // Delivery section
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SectionHeader("प्रसव / Delivery")
                        InfoRow("Delivery Type", p.deliveryType ?: "Not recorded")
                        InfoRow("Delivery Place", p.deliveryPlace ?: "Not recorded")
                        p.deliveryDate?.let  { InfoRow("Delivery Date", it) }
                        p.birthWeight?.let   { InfoRow("Baby Weight", "${it} kg") }
                    }
                }

                // JSY / PMMVY
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionHeader("सरकारी योजनाएँ / Government Schemes")

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            SchemeBox("JSY", "जननी सुरक्षा योजना", p.jsyInstallments.values.count { it }, 3, Teal, Modifier.weight(1f))
                            SchemeBox("PMMVY", "PM मातृ वंदना योजना", p.pmmvyInstallments.values.count { it }, 3, Saffron, Modifier.weight(1f))
                        }
                    }
                }

                // PNC
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SectionHeader("PNC / प्रसव-पश्चात जाँच")
                        listOf("Day 1", "Day 3", "Day 7", "Day 42").forEachIndexed { i, label ->
                            val done = p.lastVisitDate != null && i == 0
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(if (done) "✅" else "⬜", fontSize = 14.sp)
                                Text(label, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SchemeBox(code: String, name: String, received: Int, total: Int, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.background(color.copy(alpha = 0.08f), RoundedCornerShape(10.dp)).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(code, fontWeight = FontWeight.Bold, color = color, style = MaterialTheme.typography.titleSmall)
        Text(name, style = MaterialTheme.typography.labelSmall, color = TextSecondary, textAlign = TextAlign.Center)
        LinearProgressIndicator(
            progress = { received.toFloat() / total },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
        Text("$received / $total किस्तें", style = MaterialTheme.typography.labelSmall, color = TextHint)
    }
}

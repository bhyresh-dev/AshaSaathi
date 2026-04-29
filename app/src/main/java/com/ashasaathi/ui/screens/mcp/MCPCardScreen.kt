package com.ashasaathi.ui.screens.mcp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
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
import com.ashasaathi.ui.screens.households.InfoRow
import com.ashasaathi.ui.theme.Primary
import com.ashasaathi.ui.viewmodel.MCPCardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MCPCardScreen(
    navController: NavController,
    vm: MCPCardViewModel = hiltViewModel()
) {
    val patient by vm.patient.collectAsState()
    val visits by vm.ancVisits.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MCP कार्ड") },
                navigationIcon = { IconButton({ navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) } },
                actions = { IconButton({}) { Icon(Icons.Default.Share, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // MCP Card Header
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1B5E20), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        "मातृ एवं शिशु सुरक्षा कार्ड",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Mother & Child Protection Card",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            patient?.let { p ->
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("माँ की जानकारी / Mother Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Divider()
                        InfoRow("नाम / Name", p.name)
                        InfoRow("उम्र / Age", "${p.age ?: "-"} वर्ष")
                        p.rchMctsId?.let { InfoRow("RCH/MCTS ID", it) }
                        p.phone?.let { InfoRow("फोन / Phone", it) }
                        p.lmpDate?.let { InfoRow("LMP", it) }
                        p.edd?.let { InfoRow("Expected Delivery", it) }
                        p.bloodGroup?.let { InfoRow("Blood Group", it) }
                        p.gestationalAgeWeeks?.let { InfoRow("GA Weeks", it.toString()) }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("ANC विजिट / ANC Visits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        // ANC Table
                        Row(Modifier.fillMaxWidth().background(Color(0xFFE8F5E9), RoundedCornerShape(4.dp)).padding(8.dp)) {
                            listOf("विजिट", "तारीख", "BP", "Hb", "जोखिम").forEach { h ->
                                Text(h, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (visits.isEmpty()) {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                Text("कोई ANC विजिट नहीं", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        visits.take(10).forEachIndexed { i, visit ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text("ANC${i + 1}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                Text(visit.visitDate.take(10), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                val bp = if (visit.vitals.bpSystolic != null) "${visit.vitals.bpSystolic}/${visit.vitals.bpDiastolic}" else "-"
                                Text(bp, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                Text(visit.vitals.hemoglobinGdL?.toString() ?: "-", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                Text(visit.riskLevel, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

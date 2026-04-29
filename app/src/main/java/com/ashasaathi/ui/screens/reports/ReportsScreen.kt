package com.ashasaathi.ui.screens.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ashasaathi.ui.components.SectionHeader
import com.ashasaathi.ui.theme.*
import com.ashasaathi.ui.viewmodel.ReportsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    navController: NavController,
    vm: ReportsViewModel = hiltViewModel()
) {
    val state by vm.reportState.collectAsState()

    Scaffold(
        containerColor = WarmBackground,
        topBar = {
            TopAppBar(
                title = { Text("मासिक रिपोर्ट / Monthly Report", color = Color.White) },
                navigationIcon = {
                    IconButton({ navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    IconButton({ vm.exportReport() }) {
                        Icon(Icons.Default.Download, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Saffron)
            )
        }
    ) { padding ->
        if (state == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Saffron)
            }
        } else {
            val r = state!!
            Column(
                Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Period header
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Saffron, SaffronDark)),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("HMIS Monthly Report", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(r.periodLabel, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium)
                        Text("NHM — National Health Mission", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Summary grid
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionHeader("कार्यक्षेत्र सारांश / Coverage Summary")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricTile("कुल परिवार", r.totalHouseholds.toString(), Teal, Modifier.weight(1f))
                            MetricTile("कुल मरीज", r.totalPatients.toString(), Saffron, Modifier.weight(1f))
                            MetricTile("इस माह विजिट", r.visitsThisMonth.toString(), RiskGreen, Modifier.weight(1f))
                        }
                    }
                }

                // ANC
                HMISSection(
                    title = "ANC सेवाएँ / Antenatal Care",
                    rows = listOf(
                        "गर्भवती महिलाएं" to r.pregnantWomen.toString(),
                        "ANC पंजीकृत (1st Trimester)" to r.ancRegistered1stTrimester.toString(),
                        "4+ ANC प्राप्त" to r.anc4Plus.toString(),
                        "TT2 / Booster दिया" to r.ttVaccinated.toString(),
                        "IFA 180 पूर्ण" to r.ifa180Complete.toString(),
                        "उच्च जोखिम (RED)" to r.highRiskCount.toString(),
                        "संस्थागत प्रसव" to r.institutionalDeliveries.toString()
                    )
                )

                // Immunization
                HMISSection(
                    title = "UIP टीकाकरण / Immunisation",
                    rows = listOf(
                        "5 वर्ष से कम बच्चे" to r.childrenUnder5.toString(),
                        "FIC (पूर्ण टीकाकरण)" to r.ficCount.toString(),
                        "FIC % (लक्ष्य 90%)" to "${r.ficPercent}%",
                        "CIC (पूर्ण श्रेणी)" to r.cicCount.toString(),
                        "आज टीका बाकी" to r.vaccinesDueToday.toString(),
                        "छूटे टीके" to r.vaccinesMissed.toString()
                    )
                )

                // TB DOTS
                HMISSection(
                    title = "TB DOTS / क्षय रोग",
                    rows = listOf(
                        "TB मरीज पंजीकृत" to r.tbPatients.toString(),
                        "DOTS अनुपालन ≥90%" to r.dotsAdherenceGood.toString(),
                        "DOTS अनुपालन <70%" to r.dotsAdherencePoor.toString(),
                        "DBT इस माह" to r.dbtThisMonth.toString()
                    )
                )

                // NHM Activities
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SectionHeader("NHM गतिविधियाँ / Activity Codes")
                        r.activitySummary.forEach { (code, count) ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(code, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Text(count.toString(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                            Divider(color = Color(0xFFEEEEEE))
                        }
                    }
                }

                // JSY / PMMVY
                HMISSection(
                    title = "सरकारी योजनाएँ / Schemes",
                    rows = listOf(
                        "JSY लाभार्थी" to r.jsyBeneficiaries.toString(),
                        "PMMVY लाभार्थी" to r.pmmvyBeneficiaries.toString()
                    )
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun HMISSection(title: String, rows: List<Pair<String, String>>) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SectionHeader(title)
            rows.forEachIndexed { i, (label, value) ->
                Row(
                    Modifier.fillMaxWidth().background(if (i % 2 == 0) Color.White else WarmBackground).padding(vertical = 5.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = SaffronDark)
                }
            }
        }
    }
}

@Composable
private fun MetricTile(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.background(color.copy(alpha = 0.1f), RoundedCornerShape(10.dp)).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color, textAlign = TextAlign.Center)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, textAlign = TextAlign.Center)
    }
}

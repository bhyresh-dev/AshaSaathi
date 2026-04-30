package com.ashasaathi.ui.screens.reports

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ashasaathi.ui.components.SectionHeader
import com.ashasaathi.ui.strings.appStrings
import com.ashasaathi.ui.theme.*
import com.ashasaathi.ui.viewmodel.ReportsViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    navController: NavController,
    vm: ReportsViewModel = hiltViewModel()
) {
    val state       by vm.reportState.collectAsState()
    val pdfPath     by vm.pdfPath.collectAsState()
    val exportError by vm.exportError.collectAsState()
    val context     = LocalContext.current
    val snackHost   = remember { SnackbarHostState() }
    val s           = appStrings()

    // Open PDF when path is ready
    LaunchedEffect(pdfPath) {
        val path = pdfPath ?: return@LaunchedEffect
        runCatching {
            val file = File(path)
            val uri  = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure {
            snackHost.showSnackbar("PDF saved: $path\n(No PDF viewer installed)")
        }
        vm.clearPdfPath()
    }

    LaunchedEffect(exportError) {
        exportError?.let {
            snackHost.showSnackbar(it)
            vm.clearExportError()
        }
    }

    Scaffold(
        containerColor = WarmBackground,
        snackbarHost   = { SnackbarHost(snackHost) },
        topBar = {
            TopAppBar(
                title = { Text(s.reportsTitle, color = Color.White) },
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
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { vm.exportReport() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text(s.reportsPdfDownload, color = Color.White, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                // Summary grid
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionHeader(s.reportsCoverage)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricTile(s.reportsTotalHouseholds, r.totalHouseholds.toString(), Teal, Modifier.weight(1f))
                            MetricTile(s.reportsTotalPatients, r.totalPatients.toString(), Saffron, Modifier.weight(1f))
                            MetricTile(s.reportsVisitsMonth, r.visitsThisMonth.toString(), RiskGreen, Modifier.weight(1f))
                        }
                    }
                }

                HMISSection(
                    title = s.reportsANC,
                    rows = listOf(
                        s.reportsPregnant to r.pregnantWomen.toString(),
                        s.reportsANC1st to r.ancRegistered1stTrimester.toString(),
                        s.reportsANC4Plus to r.anc4Plus.toString(),
                        s.reportsTT to r.ttVaccinated.toString(),
                        s.reportsIFA180 to r.ifa180Complete.toString(),
                        s.reportsHighRisk to r.highRiskCount.toString(),
                        s.reportsInstitutional to r.institutionalDeliveries.toString()
                    )
                )

                HMISSection(
                    title = s.reportsImmunisation,
                    rows = listOf(
                        s.reportsUnder5 to r.childrenUnder5.toString(),
                        s.reportsFIC to r.ficCount.toString(),
                        s.reportsFICPercent to "${r.ficPercent}%",
                        s.reportsCIC to r.cicCount.toString(),
                        s.reportsVaccineDueToday to r.vaccinesDueToday.toString(),
                        s.reportsVaccineMissed to r.vaccinesMissed.toString()
                    )
                )

                HMISSection(
                    title = s.reportsTB,
                    rows = listOf(
                        s.reportsTBRegistered to r.tbPatients.toString(),
                        s.reportsDOTSGood to r.dotsAdherenceGood.toString(),
                        s.reportsDOTSPoor to r.dotsAdherencePoor.toString(),
                        s.reportsDBT to r.dbtThisMonth.toString()
                    )
                )

                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SectionHeader(s.reportsNHMActivities)
                        r.activitySummary.forEach { (code, count) ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(code, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Text(count.toString(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                            Divider(color = Color(0xFFEEEEEE))
                        }
                    }
                }

                HMISSection(
                    title = s.reportsSchemes,
                    rows = listOf(
                        s.reportsJSY to r.jsyBeneficiaries.toString(),
                        s.reportsPMMVY to r.pmmvyBeneficiaries.toString()
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

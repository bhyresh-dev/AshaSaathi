package com.ashasaathi.ui.screens.households

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ashasaathi.ui.navigation.Route
import com.ashasaathi.ui.theme.Primary
import com.ashasaathi.ui.viewmodel.HouseholdDetailViewModel
import com.ashasaathi.ui.screens.home.WorkplanCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseholdDetailScreen(
    navController: NavController,
    vm: HouseholdDetailViewModel = hiltViewModel()
) {
    val household by vm.household.collectAsState()
    val members by vm.members.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(household?.headOfFamily ?: "Household") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { /* add member */ }) {
                        Icon(Icons.Default.Add, "Add Member", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            household?.let { h ->
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            InfoRow("घर नंबर / House #", h.houseNumber)
                            InfoRow("गाँव / Village", h.village)
                            InfoRow("उप-केंद्र / Sub-Centre", h.subCentre)
                            InfoRow("कुल सदस्य / Total Members", h.totalMembers.toString())
                            InfoRow("जोखिम स्तर / Risk Level", h.overallRiskLevel)
                            if (h.pregnantWomenCount > 0) InfoRow("गर्भवती / Pregnant", h.pregnantWomenCount.toString())
                            if (h.childrenUnder5Count > 0) InfoRow("शिशु / Children <5", h.childrenUnder5Count.toString())
                        }
                    }
                }
                item {
                    Text("सदस्य / Members", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 8.dp))
                }
            }
            items(members, key = { it.patientId }) { patient ->
                WorkplanCard(
                    patient = patient,
                    onClick = { navController.navigate(Route.patientDetail(patient.patientId)) }
                )
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

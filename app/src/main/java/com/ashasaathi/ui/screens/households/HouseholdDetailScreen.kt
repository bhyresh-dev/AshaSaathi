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
import com.ashasaathi.ui.LocalAppLanguage
import com.ashasaathi.ui.components.InfoRow
import com.ashasaathi.ui.navigation.Route
import com.ashasaathi.ui.theme.Saffron
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
    val lang = LocalAppLanguage.current

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
                    containerColor = Saffron,
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
                            InfoRow(when(lang) { "en" -> "House #"; "kn" -> "ಮನೆ ಸಂ."; else -> "घर नंबर" }, h.houseNumber)
                            InfoRow(when(lang) { "en" -> "Village"; "kn" -> "ಗ್ರಾಮ"; else -> "गाँव" }, h.village)
                            InfoRow(when(lang) { "en" -> "Sub-Centre"; "kn" -> "ಉಪ-ಕೇಂದ್ರ"; else -> "उप-केंद्र" }, h.subCentre)
                            InfoRow(when(lang) { "en" -> "Total Members"; "kn" -> "ಒಟ್ಟು ಸದಸ್ಯರು"; else -> "कुल सदस्य" }, h.totalMembers.toString())
                            InfoRow(when(lang) { "en" -> "Risk Level"; "kn" -> "ಅಪಾಯ ಮಟ್ಟ"; else -> "जोखिम स्तर" }, h.overallRiskLevel)
                            if (h.pregnantWomenCount > 0) InfoRow(when(lang) { "en" -> "Pregnant"; "kn" -> "ಗರ್ಭಿಣಿ"; else -> "गर्भवती" }, h.pregnantWomenCount.toString())
                            if (h.childrenUnder5Count > 0) InfoRow(when(lang) { "en" -> "Children <5"; "kn" -> "5 ವರ್ಷ ಕೆಳಗಿನ"; else -> "शिशु <5" }, h.childrenUnder5Count.toString())
                        }
                    }
                }
                item {
                    Text(when(lang) { "en" -> "Members"; "kn" -> "ಸದಸ್ಯರು"; else -> "सदस्य" }, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 8.dp))
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


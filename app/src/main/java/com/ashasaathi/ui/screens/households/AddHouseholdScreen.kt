package com.ashasaathi.ui.screens.households

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ashasaathi.data.model.GeoPoint
import com.ashasaathi.data.model.Household
import com.ashasaathi.ui.theme.Saffron
import com.ashasaathi.ui.viewmodel.HouseholdsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHouseholdScreen(
    navController: NavController,
    vm: HouseholdsViewModel = hiltViewModel()
) {
    var houseNumber by remember { mutableStateOf("") }
    var headOfFamily by remember { mutableStateOf("") }
    var village by remember { mutableStateOf("") }
    var subCentre by remember { mutableStateOf("") }
    var totalMembers by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("परिवार जोड़ें") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Saffron, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(value = headOfFamily, onValueChange = { headOfFamily = it }, label = { Text("मुखिया का नाम / Head of Family") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = houseNumber, onValueChange = { houseNumber = it }, label = { Text("घर नंबर / House Number") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = village, onValueChange = { village = it }, label = { Text("गाँव / Village") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = subCentre, onValueChange = { subCentre = it }, label = { Text("उप-केंद्र / Sub-Centre") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(
                value = totalMembers, onValueChange = { totalMembers = it },
                label = { Text("कुल सदस्य / Total Members") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    saving = true
                    val uid = vm.currentWorkerId ?: return@Button
                    val h = Household(
                        workerId = uid,
                        houseNumber = houseNumber,
                        headOfFamily = headOfFamily,
                        village = village,
                        subCentre = subCentre,
                        totalMembers = totalMembers.toIntOrNull() ?: 0,
                        location = GeoPoint(),
                        overallRiskLevel = "GREEN",
                        createdBy = uid
                    )
                    vm.createHousehold(h)
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = headOfFamily.isNotBlank() && houseNumber.isNotBlank() && !saving,
                colors = ButtonDefaults.buttonColors(containerColor = Saffron)
            ) { Text("सेव करें / Save") }
        }
    }
}

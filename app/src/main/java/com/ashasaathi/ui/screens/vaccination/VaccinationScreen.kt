package com.ashasaathi.ui.screens.vaccination

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ashasaathi.ui.theme.*
import com.ashasaathi.ui.viewmodel.VaccinationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaccinationScreen(
    navController: NavController,
    vm: VaccinationViewModel = hiltViewModel()
) {
    val patients by vm.patientsWithVaccines.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("टीकाकरण ट्रैकर") },
                navigationIcon = { IconButton({ navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab, containerColor = Primary, contentColor = Color.White) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("बाकी / Due") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("हो गया / Done") })
            }
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val filtered = if (selectedTab == 0) patients.filter { !it.vaccinated } else patients.filter { it.vaccinated }
                items(filtered) { item ->
                    VaccineCard(
                        name = item.patientName,
                        vaccine = item.vaccineName,
                        due = item.scheduledDate,
                        done = item.vaccinated,
                        onMark = { vm.markVaccinated(item.patientId, item.vaccineId) }
                    )
                }
            }
        }
    }
}

@Composable
fun VaccineCard(name: String, vaccine: String, due: String, done: Boolean, onMark: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (done) Icons.Default.CheckCircle else Icons.Default.Schedule,
                null, tint = if (done) RiskGreen else RiskAmber, modifier = Modifier.size(32.dp)
            )
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(vaccine, style = MaterialTheme.typography.bodySmall, color = Primary)
                Text("Due: $due", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            if (!done) {
                Button(onClick = onMark, colors = ButtonDefaults.buttonColors(containerColor = RiskGreen), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                    Text("✓", color = Color.White)
                }
            }
        }
    }
}

data class VaccineItem(val patientId: String, val patientName: String, val vaccineId: String, val vaccineName: String, val scheduledDate: String, val vaccinated: Boolean)

package com.ashasaathi.ui.screens.tb

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.ashasaathi.ui.viewmodel.TBDotsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TBDotsScreen(
    navController: NavController,
    vm: TBDotsViewModel = hiltViewModel()
) {
    val tbPatients by vm.tbPatients.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TB DOTS ट्रैकर") },
                navigationIcon = { IconButton({ navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        if (tbPatients.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("💊", style = MaterialTheme.typography.headlineLarge)
                    Text("कोई TB मरीज नहीं", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                    Text("No TB patients registered", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tbPatients) { patient ->
                    TBPatientCard(
                        name = patient.name,
                        nikshayId = patient.nikshayId,
                        dotsDue = patient.dotsDue,
                        onRecordDots = { vm.recordDots(patient.patientId) }
                    )
                }
            }
        }
    }
}

@Composable
fun TBPatientCard(name: String, nikshayId: String, dotsDue: Boolean, onRecordDots: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (dotsDue) Color(0xFFFFEBEE) else Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("Nikshay: $nikshayId", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text(if (dotsDue) "⚠️ DOTS बाकी है" else "✅ DOTS ले लिया", style = MaterialTheme.typography.labelSmall, color = if (dotsDue) RiskRed else RiskGreen)
            }
            if (dotsDue) {
                Button(onClick = onRecordDots, colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                    Text("DOTS दर्ज करें", color = Color.White)
                }
            }
        }
    }
}

data class TBPatientDisplay(val patientId: String, val name: String, val nikshayId: String, val dotsDue: Boolean)

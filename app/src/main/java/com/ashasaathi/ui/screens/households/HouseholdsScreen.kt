package com.ashasaathi.ui.screens.households

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ashasaathi.data.model.Household
import com.ashasaathi.ui.navigation.Route
import com.ashasaathi.ui.screens.home.AppBottomBar
import com.ashasaathi.ui.strings.appStrings
import com.ashasaathi.ui.theme.*
import com.ashasaathi.ui.viewmodel.HouseholdsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseholdsScreen(
    navController: NavController,
    vm: HouseholdsViewModel = hiltViewModel()
) {
    val households by vm.filtered.collectAsState()
    val search by vm.search.collectAsState()
    val loading by vm.loading.collectAsState()
    val s = appStrings()

    Scaffold(
        topBar = {
            Column(Modifier.background(Saffron).padding(horizontal = 16.dp).padding(top = 48.dp, bottom = 12.dp)) {
                Text(s.householdsTitle, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = search,
                    onValueChange = vm::onSearch,
                    placeholder = { Text(s.householdsSearch, color = Color.White.copy(alpha = 0.6f)) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White.copy(alpha = 0.7f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White
                    ),
                    shape = RoundedCornerShape(100),
                    singleLine = true
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Route.ADD_HOUSEHOLD) },
                containerColor = Saffron
            ) { Icon(Icons.Default.Add, "Add Household", tint = Color.White) }
        },
        bottomBar = { AppBottomBar(navController) }
    ) { padding ->
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Saffron)
            }
        } else if (households.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏠", style = MaterialTheme.typography.headlineLarge)
                    Text(s.householdsEmpty, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                    TextButton(onClick = { navController.navigate(Route.ADD_HOUSEHOLD) }) {
                        Text(s.householdsAdd)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(households, key = { it.householdId }) { household ->
                    HouseholdCard(
                        household = household,
                        onClick = { navController.navigate(Route.householdDetail(household.householdId)) }
                    )
                }
            }
        }
    }
}

@Composable
fun HouseholdCard(household: Household, onClick: () -> Unit) {
    val riskColor = when (household.overallRiskLevel) {
        "RED" -> RiskRed; "YELLOW" -> RiskAmber; else -> RiskGreen
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(riskColor))
            Column(Modifier.weight(1f).padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("#${household.houseNumber}", style = MaterialTheme.typography.labelLarge, color = Saffron, fontWeight = FontWeight.Bold)
                    Text(household.village, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Text(household.headOfFamily, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HouseholdChip("👥 ${household.totalMembers}", Teal)
                    if (household.pregnantWomenCount > 0) HouseholdChip("🤰 ${household.pregnantWomenCount}", RiskAmber)
                    if (household.childrenUnder5Count > 0) HouseholdChip("👶 ${household.childrenUnder5Count}", Color(0xFF7B1FA2))
                }
            }
            Text("›", style = MaterialTheme.typography.headlineMedium, color = TextSecondary, modifier = Modifier.align(Alignment.CenterVertically).padding(end = 12.dp))
        }
    }
}

@Composable
fun HouseholdChip(label: String, color: Color) {
    Surface(shape = RoundedCornerShape(100), color = color.copy(alpha = 0.1f)) {
        Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = color)
    }
}

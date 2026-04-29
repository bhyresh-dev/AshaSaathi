package com.ashasaathi.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ashasaathi.ui.theme.Saffron
import com.ashasaathi.ui.viewmodel.ProfileSetupViewModel

@Composable
fun ProfileSetupScreen(
    onComplete: () -> Unit,
    vm: ProfileSetupViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var rchId by remember { mutableStateOf("") }
    var phcName by remember { mutableStateOf("") }
    var subCentre by remember { mutableStateOf("") }
    var village by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }

    val state by vm.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("प्रोफ़ाइल सेटअप", style = MaterialTheme.typography.headlineMedium)
        Text("Setup Profile", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))

        listOf(
            Triple("Name / नाम", name) { v: String -> name = v },
            Triple("RCH Portal ID", rchId) { v: String -> rchId = v },
            Triple("PHC Name", phcName) { v: String -> phcName = v },
            Triple("Sub-Centre", subCentre) { v: String -> subCentre = v },
            Triple("Village / गाँव", village) { v: String -> village = v },
            Triple("District / जिला", district) { v: String -> district = v },
        ).forEach { (label, value, onChange) ->
            OutlinedTextField(
                value = value,
                onValueChange = onChange,
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                singleLine = true
            )
        }

        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                vm.saveProfile(name, rchId, phcName, subCentre, village, district, onComplete)
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = name.isNotBlank() && rchId.isNotBlank() && !state.saving,
            colors = ButtonDefaults.buttonColors(containerColor = Saffron)
        ) {
            if (state.saving) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = androidx.compose.ui.graphics.Color.White, strokeWidth = 2.dp)
            else Text("सेव करें / Save Profile")
        }
    }
}

package com.ashasaathi.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ashasaathi.ui.components.SectionHeader
import com.ashasaathi.ui.navigation.Route
import com.ashasaathi.ui.strings.appStrings
import com.ashasaathi.ui.theme.*
import com.ashasaathi.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    vm: SettingsViewModel = hiltViewModel()
) {
    val worker by vm.worker.collectAsState()
    val selectedLanguage by vm.language.collectAsState()
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val s = appStrings()

    Scaffold(
        containerColor = WarmBackground,
        topBar = {
            TopAppBar(
                title = { Text(s.settingsTitle, color = Color.White) },
                navigationIcon = {
                    IconButton({ navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Saffron)
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Profile card
            worker?.let { w ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            Modifier.size(56.dp).clip(CircleShape).background(SaffronContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                w.name.take(1).uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = SaffronDark
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(w.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(w.phone, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text("${w.village}, ${w.district}", style = MaterialTheme.typography.bodySmall, color = TextHint)
                            w.aadhaarMasked?.let {
                                Text("Aadhaar: $it", style = MaterialTheme.typography.labelSmall, color = TextHint)
                            }
                        }
                    }
                }
            }

            // Language
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SectionHeader(s.settingsLanguageSection)
                    SettingsRow(
                        icon = Icons.Default.Language,
                        label = s.settingsAppLanguage,
                        value = when (selectedLanguage) { "kn" -> "ಕನ್ನಡ"; "en" -> "English"; else -> "हिंदी" },
                        onClick = { showLanguageDialog = true }
                    )
                }
            }

            // Voice models
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SectionHeader(s.settingsVoiceModels)
                    SettingsRow(
                        icon  = Icons.Default.Mic,
                        label = s.settingsVoiceModels,
                        value = "Whisper (~75 MB) + TinyLlama (~550 MB)",
                        onClick = { navController.navigate(Route.MODEL_SETUP) }
                    )
                }
            }

            // App info
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SectionHeader(s.settingsAppInfo)
                    SettingsRow(Icons.Default.Info, "Version", "1.0.0 (Build 1)", onClick = {})
                    SettingsRow(Icons.Default.WifiOff, "Offline Mode", "Firestore cache active", onClick = {})
                    SettingsRow(Icons.Default.Security, "Data Policy", "NHM compliant — local storage", onClick = {})
                }
            }

            // Logout
            Card(
                colors = CardDefaults.cardColors(containerColor = RiskRedSurface),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().clickable { showLogoutDialog = true }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Logout, null, tint = RiskRed)
                    Text(s.settingsLogout, style = MaterialTheme.typography.bodyLarge, color = RiskRed, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // Language picker dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(s.settingsSelectLanguage) },
            text = {
                Column {
                    listOf("hi" to "हिंदी", "kn" to "ಕನ್ನಡ", "en" to "English").forEach { (code, name) ->
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.setLanguage(code); showLanguageDialog = false }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(name, style = MaterialTheme.typography.bodyLarge)
                            if (selectedLanguage == code) Icon(Icons.Default.Check, null, tint = Teal)
                        }
                    }
                }
            },
            confirmButton = { TextButton({ showLanguageDialog = false }) { Text(s.settingsCloseBtn) } }
        )
    }

    // Logout confirm
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(s.settingsLogoutTitle) },
            text = { Text(s.settingsLogoutMsg) },
            confirmButton = {
                Button(
                    onClick = { vm.logout(); navController.navigate("login") { popUpTo(0) } },
                    colors = ButtonDefaults.buttonColors(containerColor = RiskRed)
                ) { Text(s.settingsLogoutYes, color = Color.White) }
            },
            dismissButton = {
                TextButton({ showLogoutDialog = false }) { Text(s.cancel) }
            }
        )
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = Saffron, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Icon(Icons.Default.ChevronRight, null, tint = TextHint, modifier = Modifier.size(18.dp))
    }
}

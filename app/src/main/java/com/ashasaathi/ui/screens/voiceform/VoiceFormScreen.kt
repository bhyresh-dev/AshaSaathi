package com.ashasaathi.ui.screens.voiceform

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ashasaathi.data.model.*
import com.ashasaathi.ui.LocalAppLanguage
import com.ashasaathi.ui.navigation.Route
import com.ashasaathi.ui.theme.*
import com.ashasaathi.ui.viewmodel.VoiceFormUiState
import com.ashasaathi.ui.viewmodel.VoiceFormViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun VoiceFormScreen(
    navController: NavController,
    vm: VoiceFormViewModel = hiltViewModel()
) {
    val uiState by vm.uiState.collectAsState()
    val isRec   by vm.isRecording.collectAsState()
    val isProc  by vm.isProcessing.collectAsState()
    val lang    = LocalAppLanguage.current
    val micPerm = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    Scaffold(
        containerColor = WarmBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (lang) { "kn" -> "ಧ್ವನಿ ನಮೂದು"; "en" -> "Voice Entry"; else -> "आवाज़ से दर्ज करें" },
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton({
                        if (isRec) vm.retryRecording()
                        navController.popBackStack()
                    }) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Saffron)
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val s = uiState) {
                is VoiceFormUiState.SelectType  -> FormTypeSelector(lang, vm::selectFormType)
                is VoiceFormUiState.Recording   -> RecordingPane(
                    state     = s,
                    lang      = lang,
                    isRec     = isRec,
                    isProc    = isProc,
                    hasMicPerm = micPerm.status.isGranted,
                    onMicClick = {
                        if (!micPerm.status.isGranted) { micPerm.launchPermissionRequest(); return@RecordingPane }
                        if (isRec) vm.stopAndExtract(s.formType)
                        else vm.startRecording(s.formType)
                    },
                    onDone    = { vm.stopAndExtract(s.formType) },
                    onCancel  = { vm.reset() }
                )
                is VoiceFormUiState.Processing  -> ProcessingPane(lang)
                is VoiceFormUiState.Review      -> ReviewPane(s, lang, vm, navController)
                is VoiceFormUiState.Saving      -> ProcessingPane(lang, saving = true)
                is VoiceFormUiState.Done        -> DonePane(s, lang) { vm.reset() }
                is VoiceFormUiState.Error       -> ErrorPane(s.message) { vm.retryRecording() }
                is VoiceFormUiState.ModelNotReady -> ModelNotReadyPane(
                    lang       = lang,
                    onBack     = { navController.popBackStack() },
                    onDownload = { navController.navigate(Route.MODEL_SETUP) }
                )
            }
        }
    }
}

// ── Form type selector ────────────────────────────────────────────────────────

@Composable
private fun FormTypeSelector(lang: String, onSelect: (VoiceFormType) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            when (lang) { "kn" -> "ಯಾವ ಫಾರ್ಮ್ ಭರ್ತಿ ಮಾಡಬೇಕು?"; "en" -> "Which form to fill?"; else -> "कौन सा फ़ॉर्म भरना है?" },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        VoiceFormType.entries.forEach { type ->
            FormTypeCard(type, lang, onSelect)
        }
    }
}

@Composable
private fun FormTypeCard(type: VoiceFormType, lang: String, onSelect: (VoiceFormType) -> Unit) {
    val title  = when (lang) { "kn" -> type.titleKn; "en" -> type.titleEn; else -> type.titleHi }
    val prompt = when (lang) { "kn" -> type.promptKn; "en" -> type.promptEn; else -> type.promptHi }

    Card(
        modifier  = Modifier.fillMaxWidth().clickable { onSelect(type) },
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(SaffronContainer),
                contentAlignment = Alignment.Center
            ) { Text(type.emoji, fontSize = 26.sp) }

            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    prompt.lines().take(3).joinToString("\n"),
                    style   = MaterialTheme.typography.bodySmall,
                    color   = TextSecondary,
                    maxLines = 3
                )
            }
            Icon(Icons.Default.Mic, null, tint = Saffron)
        }
    }
}

// ── Recording pane ────────────────────────────────────────────────────────────

@Composable
private fun RecordingPane(
    state: VoiceFormUiState.Recording,
    lang: String,
    isRec: Boolean,
    isProc: Boolean,
    hasMicPerm: Boolean,
    onMicClick: () -> Unit,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val transcriptReady = !isRec && !isProc && state.transcript.isNotBlank()

    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue  = 1f,
        targetValue   = if (isRec) 1.18f else 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "scale"
    )

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Form type badge
        Surface(color = SaffronContainer, shape = RoundedCornerShape(100)) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(state.formType.emoji, fontSize = 18.sp)
                Text(
                    when (lang) { "kn" -> state.formType.titleKn; "en" -> state.formType.titleEn; else -> state.formType.titleHi },
                    style = MaterialTheme.typography.labelLarge, color = SaffronDark, fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Numbered-point guide card
        Card(
            colors    = CardDefaults.cardColors(containerColor = Color.White),
            shape     = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier  = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Info, null, tint = Teal, modifier = Modifier.size(16.dp))
                    Text(
                        when (lang) { "kn" -> "ಹೀಗೆ ಹೇಳಿ (ಅಂಕಿ ಕ್ರಮದಲ್ಲಿ)"; "en" -> "Say in numbered points"; else -> "नंबर देकर बोलें" },
                        style = MaterialTheme.typography.labelMedium, color = Teal, fontWeight = FontWeight.SemiBold
                    )
                }
                val prompt = when (lang) { "kn" -> state.formType.promptKn; "en" -> state.formType.promptEn; else -> state.formType.promptHi }
                Text(prompt, style = MaterialTheme.typography.bodySmall, lineHeight = 20.sp)
            }
        }

        // Transcript preview
        AnimatedVisibility(state.transcript.isNotBlank()) {
            Card(
                colors   = CardDefaults.cardColors(containerColor = Color(0xFFF0FFF4)),
                shape    = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    state.transcript.take(300),
                    Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary
                )
            }
        }

        // Mic button
        Box(contentAlignment = Alignment.Center) {
            if (isRec) {
                Box(Modifier.size(130.dp).scale(pulse).clip(CircleShape).background(RiskRed.copy(alpha = 0.15f)))
            }
            if (transcriptReady) {
                Box(Modifier.size(130.dp).clip(CircleShape).background(RiskGreen.copy(alpha = 0.12f)))
            }
            FloatingActionButton(
                onClick        = if (transcriptReady) onDone else onMicClick,
                modifier       = Modifier.size(90.dp).scale(if (isRec) pulse else 1f),
                containerColor = when { transcriptReady -> RiskGreen; isRec -> RiskRed; else -> Saffron },
                shape          = CircleShape,
                elevation      = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(
                    when {
                        transcriptReady -> Icons.Default.Check
                        isRec           -> Icons.Default.Stop
                        !hasMicPerm     -> Icons.Default.MicOff
                        else            -> Icons.Default.Mic
                    },
                    contentDescription = null,
                    tint     = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }
        }

        Text(
            when {
                !hasMicPerm     -> when (lang) { "kn" -> "ಮೈಕ್ ಅನುಮತಿ ಟ್ಯಾಪ್ ಮಾಡಿ"; "en" -> "Tap to grant mic permission"; else -> "माइक अनुमति के लिए टैप करें" }
                transcriptReady -> when (lang) { "kn" -> "✓ ಟ್ಯಾಪ್ ಮಾಡಿ ಫಾರ್ಮ್ ತುಂಬಿಸಿ"; "en" -> "✓ Got it — tap to fill form"; else -> "✓ सुना — फ़ॉर्म भरने के लिए टैप करें" }
                isRec           -> when (lang) { "kn" -> "ಸುನ್ನಿಸುತ್ತಿದ್ದಾರೆ… ನಿಲ್ಲಿಸಲು ಟ್ಯಾಪ್ ಮಾಡಿ"; "en" -> "Listening… tap to stop"; else -> "सुन रहे हैं… रोकने के लिए टैप करें" }
                else            -> when (lang) { "kn" -> "ಮಾತನಾಡಲು ಮೈಕ್ ಟ್ಯಾಪ್ ಮಾಡಿ"; "en" -> "Tap mic and start speaking"; else -> "बोलने के लिए माइक टैप करें" }
            },
            style      = MaterialTheme.typography.bodyMedium,
            color      = if (transcriptReady) RiskGreen else TextSecondary,
            textAlign  = TextAlign.Center,
            fontWeight = if (transcriptReady) FontWeight.SemiBold else FontWeight.Normal
        )

        if (transcriptReady) {
            OutlinedButton(onClick = onMicClick, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Mic, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(when (lang) { "kn" -> "ಮತ್ತೆ ಹೇಳಿ"; "en" -> "Speak again"; else -> "फिर से बोलें" })
            }
        }

        TextButton(onClick = onCancel) {
            Text(when (lang) { "kn" -> "ರದ್ದುಮಾಡಿ"; "en" -> "Cancel"; else -> "रद्द करें" }, color = TextSecondary)
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ── Processing pane ───────────────────────────────────────────────────────────

@Composable
private fun ProcessingPane(lang: String, saving: Boolean = false) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(color = Saffron, modifier = Modifier.size(56.dp), strokeWidth = 4.dp)
            Text(
                when {
                    saving && lang == "en" -> "Saving…"
                    saving -> "सेव हो रहा है…"
                    lang == "en" -> "Understanding what you said…"
                    else  -> "आपकी बात समझ रहे हैं…"
                },
                style = MaterialTheme.typography.bodyLarge, color = TextSecondary
            )
        }
    }
}

// ── Model not ready pane ──────────────────────────────────────────────────────

@Composable
private fun ModelNotReadyPane(lang: String, onBack: () -> Unit, onDownload: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text("🎙️", fontSize = 56.sp)
            Text(
                when (lang) {
                    "en" -> "Speech recognition unavailable"
                    "kn" -> "ಧ್ವನಿ ಗುರುತಿಸುವಿಕೆ ಲಭ್ಯವಿಲ್ಲ"
                    else -> "आवाज़ पहचान उपलब्ध नहीं"
                },
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
            )
            Text(
                when (lang) {
                    "en" -> "Your device does not have a speech recognition engine. Download the offline Whisper model (~75 MB) to enable fully offline voice input."
                    "kn" -> "ನಿಮ್ಮ ಸಾಧನದಲ್ಲಿ ಧ್ವನಿ ಗುರುತಿಸುವ ಎಂಜಿನ್ ಇಲ್ಲ. ಆಫ್‌ಲೈನ್ ಮಾಡೆಲ್ (~75 MB) ಡೌನ್‌ಲೋಡ್ ಮಾಡಿ."
                    else -> "आपके डिवाइस में आवाज़ पहचान इंजन नहीं है। ऑफलाइन Whisper मॉडल (~75 MB) डाउनलोड करें।"
                },
                style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = TextSecondary
            )
            Button(
                onClick  = onDownload,
                colors   = ButtonDefaults.buttonColors(containerColor = Saffron),
                shape    = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(when (lang) { "en" -> "Download Voice Model"; "kn" -> "ಮಾಡೆಲ್ ಡೌನ್‌ಲೋಡ್"; else -> "मॉडल डाउनलोड करें" })
            }
            OutlinedButton(
                onClick  = onBack,
                shape    = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(when (lang) { "en" -> "Go Back"; "kn" -> "ಹಿಂದೆ ಹೋಗಿ"; else -> "वापस जाएं" })
            }
        }
    }
}

// ── Review pane ───────────────────────────────────────────────────────────────

@Composable
private fun ReviewPane(
    state: VoiceFormUiState.Review,
    lang: String,
    vm: VoiceFormViewModel,
    navController: NavController
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.CheckCircle, null, tint = RiskGreen, modifier = Modifier.size(22.dp))
            Text(
                when (lang) { "en" -> "Review & Edit"; else -> "जाँचें और ठीक करें" },
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold
            )
        }

        // Transcript card
        if (state.transcript.isNotBlank()) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4FF)), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(when (lang) { "en" -> "What you said"; else -> "आपने बोला" }, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(state.transcript.take(300), style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                }
            }
        }

        // Validation errors banner
        if (state.validationErrors.isNotEmpty()) {
            Card(
                colors   = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                shape    = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Warning, null, tint = RiskAmber, modifier = Modifier.size(18.dp))
                        Text(
                            when (lang) { "en" -> "Required fields missing:"; "kn" -> "ಅಗತ್ಯ ಕ್ಷೇತ್ರಗಳು:"; else -> "ये ज़रूरी फ़ील्ड भरें:" },
                            style = MaterialTheme.typography.labelMedium, color = RiskAmber, fontWeight = FontWeight.Bold
                        )
                    }
                    state.validationErrors.forEach { err ->
                        Text("• $err", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                    }
                }
            }
        }

        // Form fields
        when (state.formType) {
            VoiceFormType.HOUSEHOLD -> HouseholdForm(state.household ?: ExtractedHousehold(), lang, vm::updateHousehold)
            VoiceFormType.PATIENT   -> PatientForm(state.patient     ?: ExtractedPatient(),   lang, vm::updatePatient)
            VoiceFormType.ANC_VISIT -> ANCForm(state.anc             ?: ExtractedANC(),        lang, vm::updateANC)
            VoiceFormType.VACCINE   -> VaccineForm(state.vaccine     ?: ExtractedVaccine(),   lang, vm::updateVaccine)
            VoiceFormType.TB_DOTS   -> DOTSForm(state.dots           ?: ExtractedDOTS(),      lang, vm::updateDOTS)
        }

        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick  = { vm.retryRecording() },
                modifier = Modifier.weight(1f).height(52.dp),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Mic, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (lang == "en") "Re-record" else "फिर बोलें")
            }
            Button(
                onClick  = vm::submitForm,
                modifier = Modifier.weight(1f).height(52.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Saffron),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (lang == "en") "Save Form" else "सेव करें")
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ── Individual form bodies ────────────────────────────────────────────────────

@Composable
private fun HouseholdForm(h: ExtractedHousehold, lang: String, onChange: (ExtractedHousehold) -> Unit) {
    FormSection(if (lang == "en") "Household Details (Format A)" else "परिवार विवरण (फॉर्मेट A)") {
        VoiceField(if (lang == "en") "House Number *" else "घर नंबर *", h.houseNumber, required = true) { onChange(h.copy(houseNumber = it)) }
        VoiceField(if (lang == "en") "Head of Family *" else "मुखिया का नाम *", h.headOfFamily, required = true) { onChange(h.copy(headOfFamily = it)) }
        VoiceField(if (lang == "en") "Village *" else "गाँव *", h.village, required = true) { onChange(h.copy(village = it)) }
        VoiceNumberField(if (lang == "en") "Total Members *" else "कुल सदस्य *", h.totalMembers, required = true) { onChange(h.copy(totalMembers = it)) }
        VoiceNumberField(if (lang == "en") "Eligible Couples *" else "योग्य दंपति *", h.eligibleCouples, required = true) { onChange(h.copy(eligibleCouples = it)) }
        VoiceNumberField(if (lang == "en") "Pregnant Women" else "गर्भवती महिलाएं", h.pregnantWomen) { onChange(h.copy(pregnantWomen = it)) }
        VoiceNumberField(if (lang == "en") "Children < 5 yrs" else "5 साल से कम बच्चे", h.childrenUnder5) { onChange(h.copy(childrenUnder5 = it)) }
        VoiceNumberField(if (lang == "en") "Elderly (60+)" else "बुजुर्ग (60+)", h.elderly) { onChange(h.copy(elderly = it)) }
    }
}

@Composable
private fun PatientForm(p: ExtractedPatient, lang: String, onChange: (ExtractedPatient) -> Unit) {
    FormSection(if (lang == "en") "Beneficiary Registration (RCH Format)" else "लाभार्थी पंजीकरण (RCH फॉर्मेट)") {
        VoiceField(if (lang == "en") "Patient Name *" else "महिला का नाम *", p.name, required = true) { onChange(p.copy(name = it)) }
        VoiceField(if (lang == "en") "Husband Name *" else "पति का नाम *", p.husbandName, required = true) { onChange(p.copy(husbandName = it)) }
        VoiceNumberField(if (lang == "en") "Age (years) *" else "उम्र (वर्ष) *", p.age, required = true) { onChange(p.copy(age = it)) }
        VoiceField(if (lang == "en") "Phone Number" else "फोन नंबर", p.phone, keyboardType = KeyboardType.Phone) { onChange(p.copy(phone = it)) }
        VoiceField(if (lang == "en") "Village *" else "गाँव *", p.village, required = true) { onChange(p.copy(village = it)) }
        VoiceField(if (lang == "en") "RCH/MCTS ID" else "RCH/MCTS आईडी", p.rchId) { onChange(p.copy(rchId = it)) }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (lang == "en") "Pregnant" else "गर्भवती", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Switch(checked = p.isPregnant, onCheckedChange = { onChange(p.copy(isPregnant = it)) }, colors = SwitchDefaults.colors(checkedThumbColor = Saffron, checkedTrackColor = SaffronContainer))
        }
        if (p.isPregnant) {
            VoiceField(if (lang == "en") "LMP Date (YYYY-MM-DD)" else "अंतिम माहवारी (YYYY-MM-DD)", p.lmpDate) { onChange(p.copy(lmpDate = it)) }
        }
    }
}

@Composable
private fun ANCForm(a: ExtractedANC, lang: String, onChange: (ExtractedANC) -> Unit) {
    FormSection(if (lang == "en") "ANC Visit — HMIS Format" else "ANC विजिट — HMIS फॉर्मेट") {
        VoiceField(if (lang == "en") "Patient Name *" else "महिला का नाम *", a.patientName, required = true) { onChange(a.copy(patientName = it)) }
        VoiceField(if (lang == "en") "LMP Date *" else "अंतिम माहवारी *", a.lmpDate, required = true) { onChange(a.copy(lmpDate = it)) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VoiceField("BP Systolic *", a.bpSystolic?.toString() ?: "", required = true, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f)) { onChange(a.copy(bpSystolic = it.toIntOrNull())) }
            VoiceField("BP Diastolic *", a.bpDiastolic?.toString() ?: "", required = true, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f)) { onChange(a.copy(bpDiastolic = it.toIntOrNull())) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VoiceField(if (lang == "en") "Weight (kg) *" else "वजन (kg) *", a.weightKg?.toString() ?: "", required = true, keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f)) { onChange(a.copy(weightKg = it.toDoubleOrNull())) }
            VoiceField("Hb (g/dL) *", a.hemoglobinGdL?.toString() ?: "", required = true, keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f)) { onChange(a.copy(hemoglobinGdL = it.toDoubleOrNull())) }
        }
        VoiceNumberField(if (lang == "en") "IFA Tablets given today" else "IFA गोलियां (आज)", a.ifaTabletsGiven) { onChange(a.copy(ifaTabletsGiven = it)) }
        VoiceField(if (lang == "en") "TT Dose (TT1/TT2/BOOSTER)" else "TT डोज", a.ttDose) { onChange(a.copy(ttDose = it)) }
        VoiceField(if (lang == "en") "Urine Protein" else "पेशाब प्रोटीन", a.urineProtein) { onChange(a.copy(urineProtein = it)) }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (lang == "en") "Fever" else "बुखार", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Switch(checked = a.hasFever, onCheckedChange = { onChange(a.copy(hasFever = it)) }, colors = SwitchDefaults.colors(checkedThumbColor = RiskRed, checkedTrackColor = RiskRedSurface))
        }
        VoiceField(if (lang == "en") "Complaints / Notes" else "समस्याएं / नोट्स", a.complaints, singleLine = false) { onChange(a.copy(complaints = it)) }
    }
}

@Composable
private fun VaccineForm(v: ExtractedVaccine, lang: String, onChange: (ExtractedVaccine) -> Unit) {
    FormSection(if (lang == "en") "Immunisation Record (UIP)" else "टीकाकरण दर्ज (UIP)") {
        VoiceField(if (lang == "en") "Child Name *" else "बच्चे का नाम *", v.childName, required = true) { onChange(v.copy(childName = it)) }
        VoiceField(if (lang == "en") "Date of Birth *" else "जन्म तिथि *", v.dob, required = true) { onChange(v.copy(dob = it)) }
        VoiceField(if (lang == "en") "Vaccine Name *" else "टीके का नाम *", v.vaccineName, required = true) { onChange(v.copy(vaccineName = it)) }
        VoiceField(if (lang == "en") "Mother Name" else "माँ का नाम", v.motherName) { onChange(v.copy(motherName = it)) }
        VoiceField(if (lang == "en") "Village" else "गाँव", v.village) { onChange(v.copy(village = it)) }
    }
}

@Composable
private fun DOTSForm(d: ExtractedDOTS, lang: String, onChange: (ExtractedDOTS) -> Unit) {
    FormSection(if (lang == "en") "TB DOTS Record (Nikshay)" else "TB DOTS दर्ज (निक्षय)") {
        VoiceField(if (lang == "en") "Patient Name *" else "मरीज़ का नाम *", d.patientName, required = true) { onChange(d.copy(patientName = it)) }
        VoiceField(if (lang == "en") "Nikshay ID *" else "निक्षय आईडी *", d.nikshayId, required = true) { onChange(d.copy(nikshayId = it)) }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (lang == "en") "DOTS taken today?" else "आज DOTS लिया?", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Switch(checked = d.dotsTaken, onCheckedChange = { onChange(d.copy(dotsTaken = it)) }, colors = SwitchDefaults.colors(checkedThumbColor = RiskGreen, checkedTrackColor = RiskGreenSurface))
        }
        VoiceField(if (lang == "en") "Side Effects (if any)" else "दुष्प्रभाव (यदि हो)", d.sideEffects, singleLine = false) { onChange(d.copy(sideEffects = it)) }
    }
}

// ── Done / Error panes ────────────────────────────────────────────────────────

@Composable
private fun DonePane(state: VoiceFormUiState.Done, lang: String, onReset: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text("✅", fontSize = 64.sp)
            Text(
                if (lang == "en") "Saved!" else "सेव हो गया!",
                style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = RiskGreen
            )
            Text(state.message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick  = onReset,
                colors   = ButtonDefaults.buttonColors(containerColor = Saffron),
                shape    = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text(if (lang == "en") "New Entry" else "नई एंट्री") }
        }
    }
}

@Composable
private fun ErrorPane(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text("🎙️", fontSize = 56.sp)
            Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = RiskRed)
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Saffron)) {
                Icon(Icons.Default.Mic, null); Spacer(Modifier.width(6.dp)); Text("फिर से बोलें / Retry")
            }
        }
    }
}

// ── Reusable field components ─────────────────────────────────────────────────

@Composable
private fun FormSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier  = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SaffronDark)
            HorizontalDivider(color = Color(0xFFEEEEEE))
            content()
        }
    }
}

@Composable
private fun VoiceField(
    label: String,
    value: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    required: Boolean  = false,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value          = value,
        onValueChange  = onChange,
        label          = { Text(if (required && value.isBlank()) "⚠ $label" else label) },
        modifier       = modifier,
        singleLine     = singleLine,
        isError        = required && value.isBlank(),
        minLines       = if (singleLine) 1 else 2,
        maxLines       = if (singleLine) 1 else 4,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors         = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = Saffron,
            unfocusedBorderColor = Color(0xFFDDDDDD),
            errorBorderColor     = RiskAmber
        )
    )
}

@Composable
private fun VoiceNumberField(
    label: String,
    value: Int?,
    modifier: Modifier = Modifier.fillMaxWidth(),
    required: Boolean  = false,
    onChange: (Int?) -> Unit
) {
    OutlinedTextField(
        value          = value?.toString() ?: "",
        onValueChange  = { onChange(it.toIntOrNull()) },
        label          = { Text(if (required && value == null) "⚠ $label" else label) },
        modifier       = modifier,
        singleLine     = true,
        isError        = required && value == null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors         = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = Saffron,
            unfocusedBorderColor = Color(0xFFDDDDDD),
            errorBorderColor     = RiskAmber
        )
    )
}

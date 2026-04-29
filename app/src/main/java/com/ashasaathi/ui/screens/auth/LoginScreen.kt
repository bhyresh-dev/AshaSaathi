package com.ashasaathi.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ashasaathi.ui.theme.Saffron
import com.ashasaathi.ui.theme.SaffronDark
import com.ashasaathi.ui.viewmodel.LoginViewModel

private data class LangOption(val code: String, val label: String, val sub: String)

private val LANGUAGES = listOf(
    LangOption("hi", "हिंदी",   "Hindi"),
    LangOption("kn", "ಕನ್ನಡ",  "Kannada"),
    LangOption("en", "English", "अंग्रेज़ी")
)

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    vm: LoginViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(state.isLoggedIn) {
        if (state.isLoggedIn) onLoggedIn()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Saffron, SaffronDark))
            )
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(64.dp))

        Text("🌿", fontSize = 56.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            "आशा साथी",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            "ASHA Worker Login",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f)
        )

        Spacer(Modifier.height(28.dp))

        // ── Language selector ─────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LANGUAGES.forEach { lang ->
                val selected = state.selectedLanguage == lang.code
                OutlinedButton(
                    onClick  = { vm.onLanguageSelect(lang.code) },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selected) Color.White else Color.Transparent,
                        contentColor   = if (selected) SaffronDark else Color.White
                    ),
                    border   = BorderStroke(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) Color.White else Color.White.copy(alpha = 0.5f)
                    )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(lang.label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            style = MaterialTheme.typography.bodyMedium)
                        Text(lang.sub, style = MaterialTheme.typography.labelSmall,
                            color = if (selected) SaffronDark.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.6f))
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Login card ────────────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors   = CardDefaults.cardColors(containerColor = Color.White),
            shape    = MaterialTheme.shapes.large
        ) {
            Column(Modifier.padding(24.dp)) {
                AnimatedVisibility(!state.otpSent) {
                    Column {
                        Text(
                            when (state.selectedLanguage) {
                                "kn" -> "ಮೊಬೈಲ್ ನಂಬರ್ ನಮೂದಿಸಿ"
                                "en" -> "Enter Mobile Number"
                                else -> "मोबाइल नंबर दर्ज करें"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        OutlinedTextField(
                            value           = state.phone,
                            onValueChange   = vm::onPhoneChange,
                            label           = { Text("Mobile Number") },
                            prefix          = { Text("+91 ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier        = Modifier.fillMaxWidth(),
                            singleLine      = true,
                            isError         = state.error != null
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick  = vm::sendOtp,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            enabled  = state.phone.length == 10 && !state.loading,
                            colors   = ButtonDefaults.buttonColors(containerColor = Saffron)
                        ) {
                            if (state.loading) CircularProgressIndicator(
                                modifier    = Modifier.size(20.dp),
                                color       = Color.White,
                                strokeWidth = 2.dp
                            ) else Text(
                                when (state.selectedLanguage) {
                                    "kn" -> "OTP ಕಳುಹಿಸಿ"
                                    "en" -> "Send OTP"
                                    else -> "OTP भेजें"
                                }
                            )
                        }
                    }
                }

                AnimatedVisibility(state.otpSent) {
                    Column {
                        Text(
                            when (state.selectedLanguage) {
                                "kn" -> "OTP ನಮೂದಿಸಿ"
                                "en" -> "Enter OTP"
                                else -> "OTP दर्ज करें"
                            },
                            style    = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            "+91 ${state.phone}",
                            style  = MaterialTheme.typography.bodySmall,
                            color  = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        OutlinedTextField(
                            value           = state.otp,
                            onValueChange   = vm::onOtpChange,
                            label           = { Text("6-digit OTP") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier        = Modifier.fillMaxWidth(),
                            singleLine      = true,
                            isError         = state.error != null
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick  = vm::verifyOtp,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            enabled  = state.otp.length == 6 && !state.loading,
                            colors   = ButtonDefaults.buttonColors(containerColor = Saffron)
                        ) {
                            if (state.loading) CircularProgressIndicator(
                                modifier    = Modifier.size(20.dp),
                                color       = Color.White,
                                strokeWidth = 2.dp
                            ) else Text(
                                when (state.selectedLanguage) {
                                    "kn" -> "ಪರಿಶೀಲಿಸಿ"
                                    "en" -> "Verify & Login"
                                    else -> "सत्यापित करें"
                                }
                            )
                        }
                        TextButton(
                            onClick  = vm::resendOtp,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                when (state.selectedLanguage) {
                                    "kn" -> "OTP ಮತ್ತೆ ಕಳುಹಿಸಿ"
                                    "en" -> "Resend OTP"
                                    else -> "OTP फिर भेजें"
                                }
                            )
                        }
                    }
                }

                state.error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            when (state.selectedLanguage) {
                "kn" -> "ಭಾಷೆ ನಂತರ ಸೆಟ್ಟಿಂಗ್‌ಗಳಲ್ಲಿ ಬದಲಾಯಿಸಬಹುದು"
                "en" -> "Language can be changed in Settings"
                else -> "भाषा बाद में Settings में बदल सकते हैं"
            },
            style  = MaterialTheme.typography.bodySmall,
            color  = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

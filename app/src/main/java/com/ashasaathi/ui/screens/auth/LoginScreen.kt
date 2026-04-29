package com.ashasaathi.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ashasaathi.ui.theme.Primary
import com.ashasaathi.ui.viewmodel.LoginViewModel

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
            .background(Primary)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(80.dp))

        Text("🌿", fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
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

        Spacer(Modifier.height(48.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = MaterialTheme.shapes.large
        ) {
            Column(Modifier.padding(24.dp)) {
                AnimatedVisibility(!state.otpSent) {
                    Column {
                        Text(
                            "मोबाइल नंबर दर्ज करें",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        OutlinedTextField(
                            value = state.phone,
                            onValueChange = vm::onPhoneChange,
                            label = { Text("Mobile Number") },
                            prefix = { Text("+91 ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = state.error != null
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = vm::sendOtp,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            enabled = state.phone.length == 10 && !state.loading,
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            if (state.loading) CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            else Text("OTP भेजें / Send OTP")
                        }
                    }
                }

                AnimatedVisibility(state.otpSent) {
                    Column {
                        Text(
                            "OTP दर्ज करें",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            "+91 ${state.phone} पर OTP भेजा गया",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        OutlinedTextField(
                            value = state.otp,
                            onValueChange = vm::onOtpChange,
                            label = { Text("6-digit OTP") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = state.error != null
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = vm::verifyOtp,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            enabled = state.otp.length == 6 && !state.loading,
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            if (state.loading) CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            else Text("Verify & Login")
                        }
                        TextButton(
                            onClick = vm::resendOtp,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("OTP फिर भेजें / Resend OTP")
                        }
                    }
                }

                state.error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

package com.ashasaathi.ui.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashasaathi.data.repository.UserPreferencesRepository
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class LoginState(
    val phone: String = "",
    val otp: String = "",
    val otpSent: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val selectedLanguage: String = "hi"
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val prefs: UserPreferencesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    fun onPhoneChange(v: String) { _state.value = _state.value.copy(phone = v.filter { it.isDigit() }.take(10), error = null) }
    fun onOtpChange(v: String) { _state.value = _state.value.copy(otp = v.filter { it.isDigit() }.take(6), error = null) }

    fun onLanguageSelect(lang: String) {
        _state.value = _state.value.copy(selectedLanguage = lang)
        viewModelScope.launch { prefs.setLanguage(lang) }
    }

    fun sendOtp(activity: Activity? = null) {
        val phone = "+91${_state.value.phone}"
        _state.value = _state.value.copy(loading = true, error = null)

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                signInWithCredential(credential)
            }
            override fun onVerificationFailed(e: FirebaseException) {
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
            override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                verificationId = id
                resendToken = token
                _state.value = _state.value.copy(loading = false, otpSent = true)
            }
        }

        // For production: pass real Activity. For now use test mode if activity is null.
        val optBuilder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setCallbacks(callbacks)

        activity?.let { optBuilder.setActivity(it) }
        PhoneAuthProvider.verifyPhoneNumber(optBuilder.build())
    }

    fun verifyOtp() {
        val id = verificationId ?: return
        val credential = PhoneAuthProvider.getCredential(id, _state.value.otp)
        signInWithCredential(credential)
    }

    fun resendOtp(activity: Activity? = null) {
        _state.value = _state.value.copy(otpSent = false, otp = "")
        sendOtp(activity)
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { auth.signInWithCredential(credential).await() }
                .onSuccess { _state.value = _state.value.copy(loading = false, isLoggedIn = true) }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.message) }
        }
    }
}

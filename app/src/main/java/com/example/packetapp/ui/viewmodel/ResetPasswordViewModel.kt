package com.example.packetapp.ui.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.packetapp.network.KtorClient
import kotlinx.coroutines.launch

data class ResetPasswordUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

class ResetPasswordViewModel : ViewModel() {
    val uiState = mutableStateOf(ResetPasswordUiState())
    private var hasNavigated = false

    fun resetPassword(code: String, newPassword: String, onPasswordReset: () -> Unit) {
        if (hasNavigated) {
            println("ResetPassword already navigated, ignoring")
            return
        }

        viewModelScope.launch {
            println("ResetPassword attempt with code: $code")
            uiState.value = uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                KtorClient.apiService.resetPassword(code, newPassword)
                uiState.value = uiState.value.copy(isLoading = false, isSuccess = true)
                hasNavigated = true
                println("Calling onPasswordReset")
                onPasswordReset()
            } catch (e: Exception) {
                println("ResetPassword failed: ${e.message}")
                uiState.value = uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Ошибка соединения"
                )
            }
        }
    }

    fun resetState() {
        uiState.value = ResetPasswordUiState()
        hasNavigated = false
    }
}

class ResetPasswordViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ResetPasswordViewModel::class.java)) {
            return ResetPasswordViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
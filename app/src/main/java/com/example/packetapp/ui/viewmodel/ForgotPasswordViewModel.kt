package com.example.packetapp.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.packetapp.network.KtorClient
import io.ktor.client.plugins.*
import io.ktor.http.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ForgotPasswordViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState

    fun initialize(context: Context) {
        // Пока не требуется, но оставляем для совместимости
    }

    fun requestResetCode(email: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val message = KtorClient.apiService.requestResetCode(email)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSuccess = true,
                    successMessage = message
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Ошибка соединения"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}

data class ForgotPasswordUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
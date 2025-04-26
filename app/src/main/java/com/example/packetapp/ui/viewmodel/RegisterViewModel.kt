package com.example.packetapp.ui.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.packetapp.network.KtorClient
import io.ktor.client.plugins.*
import io.ktor.http.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState

    private lateinit var sharedPreferences: SharedPreferences

    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    }

    fun register(name: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val response = KtorClient.apiService.register(name, email, password)
                // Сохраняем токены
                sharedPreferences.edit()
                    .putString("access_token", response.token)
                    .putString("refresh_token", response.refreshToken)
                    .putInt("user_id", response.user.id)
                    .putString("email", email)
                    .apply()
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
            } catch (e: ClientRequestException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = if (e.response.status == HttpStatusCode.Unauthorized) {
                        "Неверные учетные данные"
                    } else {
                        e.response.status.description
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Ошибка соединения"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

data class RegisterUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)
package com.example.packetapp.ui.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.packetapp.network.KtorClient
//import io.ktor.client.network.socket.*
import io.ktor.client.plugins.*
import io.ktor.http.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    private lateinit var sharedPreferences: SharedPreferences

    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val response = KtorClient.apiService.login(email, password)
                sharedPreferences.edit()
                    .putString("access_token", response.token)
                    .putString("refresh_token", response.refreshToken)
                    .putInt("user_id", response.user.id)
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
                val errorMessage = when (e.message) {
                    "Invalid credentials" -> "Неверные учетные данные"
                    else -> e.message ?: "Ошибка соединения"
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = errorMessage
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

data class LoginUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)
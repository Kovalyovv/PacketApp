package com.example.packetapp.ui.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.packetapp.data.AuthManager
import com.example.packetapp.network.ApiService
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

class LoginViewModel(
    private val apiService: ApiService,
    private val authManager: AuthManager // Используем AuthManager вместо SharedPreferences
) : ViewModel() {

    private val _uiState = mutableStateOf(LoginUiState())
    val uiState get() = _uiState

    fun login(email: String, password: String, onLoginSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val loginResponse = apiService.login(email, password)
                // Сохраняем данные через AuthManager
                authManager.saveAuthData(
                    accessToken = loginResponse.token,
                    refreshToken = loginResponse.refreshToken,
                    userId = loginResponse.user.id,
                    name = loginResponse.user.name,
                    email = loginResponse.user.email
                )
                _uiState.value = _uiState.value.copy(isSuccess = true, isLoading = false)
                onLoginSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Неизвестная ошибка",
                    isLoading = false
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState()
    }
}

class LoginViewModelFactory(
    private val apiService: ApiService,
    private val authManager: AuthManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(apiService, authManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
package com.example.packetapp.ui.viewmodel

import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.packetapp.network.KtorClient
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

class LoginViewModel(
    private val sharedPreferences: SharedPreferences
) : ViewModel() {
    val uiState = mutableStateOf(LoginUiState())
    private var hasNavigated = false

    fun login(email: String, password: String, onLoginSuccess: () -> Unit) {
        if (hasNavigated) {
            println("Login already navigated, ignoring")
            return
        }

        viewModelScope.launch {
            println("Login attempt with email: $email")
            uiState.value = uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val response = KtorClient.apiService.login(email, password)
                sharedPreferences.edit()
                    .putString("access_token", response.token)
                    .putString("refresh_token", response.refreshToken)
                    .putInt("user_id", response.user.id)
                    .apply()
                println("Tokens saved: access=${response.token}, refresh=${response.refreshToken}, userId=${response.user.id}")
                uiState.value = uiState.value.copy(isLoading = false, isSuccess = true)
                hasNavigated = true
                println("Calling onLoginSuccess")
                onLoginSuccess()
            } catch (e: Exception) {
                println("Login failed: ${e.message}")
                uiState.value = uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Ошибка соединения"
                )
            }
        }
    }

    fun resetState() {
        uiState.value = LoginUiState()
        hasNavigated = false
    }
}

class LoginViewModelFactory(
    private val sharedPreferences: SharedPreferences
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(sharedPreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
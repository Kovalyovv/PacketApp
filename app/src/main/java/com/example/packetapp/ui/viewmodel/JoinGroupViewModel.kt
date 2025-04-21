package com.example.packetapp.ui.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.packetapp.data.AuthManager
import com.example.packetapp.network.KtorClient
import kotlinx.coroutines.launch

data class JoinGroupUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

class JoinGroupViewModel(private val authManager: AuthManager) : ViewModel() {
    private val _uiState = mutableStateOf(JoinGroupUiState())
    val uiState: State<JoinGroupUiState> = _uiState

    fun joinGroup(inviteCode: String) {
        viewModelScope.launch {
            _uiState.value = JoinGroupUiState(isLoading = true, errorMessage = null)
            try {
                val accessToken = authManager.getAccessToken() ?: throw Exception("Токен отсутствует")
                KtorClient.apiService.joinGroup(accessToken, inviteCode)
                _uiState.value = JoinGroupUiState(isSuccess = true)
            } catch (e: Exception) {
                if (e.message == "Токен недействителен") {
                    try {
                        val refreshToken = authManager.getRefreshToken() ?: throw Exception("Refresh-токен отсутствует")
                        val response = KtorClient.apiService.refreshToken(refreshToken)
                        authManager.saveAuthData(response.token, response.refreshToken, response.user.id)
                        val newAccessToken = authManager.getAccessToken()!!
                        KtorClient.apiService.joinGroup(newAccessToken, inviteCode)
                        _uiState.value = JoinGroupUiState(isSuccess = true)
                    } catch (refreshError: Exception) {
                        _uiState.value = JoinGroupUiState(
                            errorMessage = "Ошибка авторизации: ${refreshError.message}"
                        )
                    }
                } else {
                    _uiState.value = JoinGroupUiState(
                        errorMessage = e.message ?: "Ошибка присоединения к группе"
                    )
                }
            }
        }
    }
}
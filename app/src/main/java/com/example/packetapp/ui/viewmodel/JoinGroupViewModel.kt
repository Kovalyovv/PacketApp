package com.example.packetapp.ui.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.packetapp.data.AuthManager
import com.example.packetapp.network.KtorClient
import com.example.packetapp.models.JoinGroupRequest
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
                val userId = authManager.getUserId() ?: throw IllegalStateException("Пользователь не авторизован")
                val request = JoinGroupRequest(userId = userId, inviteCode = inviteCode)
                KtorClient.apiService.joinGroup(accessToken, request)
                _uiState.value = JoinGroupUiState(isSuccess = true)
            } catch (e: Exception) {
                if (e.message == "Токен недействителен") {
                    try {
                        val refreshToken = authManager.getRefreshToken() ?: throw Exception("Refresh-токен отсутствует")
                        val response = KtorClient.apiService.refreshToken(refreshToken)
                        authManager.saveAuthData(response.token, response.refreshToken, response.user.id, response.user.name, response.user.email)
                        val newAccessToken = authManager.getAccessToken()!!
                        val userId = authManager.getUserId() ?: throw IllegalStateException("Пользователь не авторизован")
                        val request = JoinGroupRequest(userId = userId, inviteCode = inviteCode)
                        KtorClient.apiService.joinGroup(newAccessToken, request)
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

    fun clearState() {
        _uiState.value = JoinGroupUiState()
    }
}
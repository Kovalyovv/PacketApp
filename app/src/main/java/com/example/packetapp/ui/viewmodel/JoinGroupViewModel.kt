package com.example.packetapp.ui.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.packetapp.data.AuthManager
import com.example.packetapp.network.KtorClient
import com.example.packetapp.models.JoinGroupRequest
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import kotlinx.coroutines.launch

data class JoinGroupUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val errorStatus: Int? = null // Добавляем статус ошибки
)

class JoinGroupViewModel(private val authManager: AuthManager) : ViewModel() {
    private val _uiState = mutableStateOf(JoinGroupUiState())
    val uiState: State<JoinGroupUiState> = _uiState

    fun joinGroup(inviteCode: String) {
        viewModelScope.launch {
            _uiState.value = JoinGroupUiState(isLoading = true, errorMessage = null, errorStatus = null)
            try {
                val accessToken = authManager.getAccessToken() ?: throw Exception("Токен отсутствует")
                val userId = authManager.getUserId() ?: throw IllegalStateException("Пользователь не авторизован")
                val request = JoinGroupRequest(userId = userId, inviteCode = inviteCode)
                KtorClient.apiService.joinGroup(accessToken, request)
                _uiState.value = JoinGroupUiState(isSuccess = true, isLoading = false)
            } catch (e: ClientRequestException) {
                val status = e.response.status.value
                val errorMessage = e.response.body<Map<String, String>>()["error"] ?: e.message
                _uiState.value = when (status) {
                    409 -> JoinGroupUiState(
                        isLoading = false,
                        errorMessage = "Вы уже состоите в этой группе",
                        errorStatus = status
                    )
                    401 -> {
                        try {
                            val refreshToken = authManager.getRefreshToken() ?: throw Exception("Refresh-токен отсутствует")
                            val refreshResponse = KtorClient.apiService.refreshToken(refreshToken)
                            authManager.saveAuthData(
                                refreshResponse.token,
                                refreshResponse.refreshToken,
                                refreshResponse.user.id,
                                refreshResponse.user.name,
                                refreshResponse.user.email
                            )
                            val newAccessToken = authManager.getAccessToken()!!
                            val userId = authManager.getUserId() ?: throw IllegalStateException("Пользователь не авторизован")
                            val request = JoinGroupRequest(userId = userId, inviteCode = inviteCode)
                            KtorClient.apiService.joinGroup(newAccessToken, request)
                            JoinGroupUiState(isSuccess = true, isLoading = false)
                        } catch (refreshError: Exception) {
                            JoinGroupUiState(
                                isLoading = false,
                                errorMessage = "Ошибка авторизации: ${refreshError.message}",
                                errorStatus = 401
                            )
                        }
                    }
                    else -> JoinGroupUiState(
                        isLoading = false,
                        errorMessage = errorMessage ?: "Ошибка присоединения к группе",
                        errorStatus = status
                    )
                }
            } catch (e: Exception) {
                _uiState.value = JoinGroupUiState(
                    isLoading = false,
                    errorMessage = e.message ?: "Ошибка присоединения к группе",
                    errorStatus = null
                )
            }
        }
    }

    fun clearState() {
        _uiState.value = JoinGroupUiState()
    }
}
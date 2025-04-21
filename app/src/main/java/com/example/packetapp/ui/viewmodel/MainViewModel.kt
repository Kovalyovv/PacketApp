package com.example.packetapp.ui.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.packetapp.data.AuthManager
import com.example.packetapp.models.GroupSummary
import com.example.packetapp.network.KtorClient
import kotlinx.coroutines.launch

data class MainUiState(
    val groupSummaries: List<GroupSummary> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class MainViewModel(
    private val authManager: AuthManager
) : ViewModel() {
    val uiState = mutableStateOf(MainUiState())

    init {
        loadGroupSummaries()
    }

    fun loadGroupSummaries() {
        viewModelScope.launch {
            uiState.value = uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val accessToken = authManager.getAccessToken() ?: throw Exception("Токен отсутствует")
                val summaries = KtorClient.apiService.getGroupSummaries(accessToken)
                uiState.value = uiState.value.copy(groupSummaries = summaries, isLoading = false)
            } catch (e: Exception) {
                if (e.message == "Токен недействителен") {
                    try {
                        val refreshToken = authManager.getRefreshToken() ?: throw Exception("Refresh-токен отсутствует")
                        val response = KtorClient.apiService.refreshToken(refreshToken)
                        authManager.saveAuthData(response.token, response.refreshToken, response.user.id)
                        val newAccessToken = authManager.getAccessToken()!!
                        val summaries = KtorClient.apiService.getGroupSummaries(newAccessToken)
                        uiState.value = uiState.value.copy(groupSummaries = summaries, isLoading = false)
                    } catch (refreshError: Exception) {
                        uiState.value = uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Ошибка авторизации: ${refreshError.message}"
                        )
                    }
                } else {
                    uiState.value = uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Ошибка загрузки данных"
                    )
                }
            }
        }
    }
}
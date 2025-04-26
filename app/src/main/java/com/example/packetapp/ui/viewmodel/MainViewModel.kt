package com.example.packetapp.ui.viewmodel

import androidx.compose.runtime.State
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

class MainViewModel(private val authManager: AuthManager) : ViewModel() {
    private val _uiState = mutableStateOf(MainUiState())
    val uiState: State<MainUiState> = _uiState

    init {
        loadGroups()
    }

    fun loadGroups() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val accessToken = getValidAccessToken()
                val groups = KtorClient.apiService.getUserGroups(accessToken)
                _uiState.value = _uiState.value.copy(groupSummaries = groups, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Ошибка загрузки групп"
                )
            }
        }
    }

    private suspend fun getValidAccessToken(): String {
        var accessToken = authManager.getAccessToken() ?: throw Exception("Токен отсутствует")
        try {
            KtorClient.apiService.getUserGroups(accessToken)
            return accessToken
        } catch (e: Exception) {
            if (e.message == "Токен недействителен") {
                val refreshToken = authManager.getRefreshToken() ?: throw Exception("Refresh-токен отсутствует")
                val response = KtorClient.apiService.refreshToken(refreshToken)
                authManager.saveAuthData(response.token, response.refreshToken, response.user.id, response.user.name, response.user.email)
                return authManager.getAccessToken() ?: throw Exception("Не удалось обновить токен")
            } else {
                throw e
            }
        }
    }
}
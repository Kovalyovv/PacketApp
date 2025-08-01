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
                val groups = KtorClient.apiService.getUserGroups(accessToken) ?: emptyList()
                groups.forEach { group ->
                    if (group.groupName.isEmpty()) {
                        println("Warning: Group with ID ${group.groupId} has empty name")
                    }
                    group.lastActivity?.let { activity ->
                        if (activity.type.isEmpty()) {
                            println("Warning: Activity in group ${group.groupId} has empty type")
                        }
                        if (activity.itemId == 0) { // Проверка на некорректный itemId
                            println("Warning: Activity in group ${group.groupId} has invalid itemId")
                        }
                    }
                }
                _uiState.value = _uiState.value.copy(groupSummaries = groups, isLoading = false)
            } catch (e: Exception) {
                println("Error loading groups: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    groupSummaries = emptyList(),
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
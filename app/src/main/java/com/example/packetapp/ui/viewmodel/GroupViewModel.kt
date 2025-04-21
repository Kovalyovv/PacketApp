package com.example.packetapp.ui.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.packetapp.data.AuthManager
import com.example.packetapp.models.GroupListItem
import com.example.packetapp.network.KtorClient
import kotlinx.coroutines.launch

data class GroupUiState(
    val items: List<GroupListItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class GroupViewModel(
    private val authManager: AuthManager,
    private val groupId: Int
) : ViewModel() {
    private val _uiState = mutableStateOf(GroupUiState())
    val uiState: State<GroupUiState> = _uiState

    init {
        loadItems()
    }

    fun loadItems() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val accessToken = getValidAccessToken()
                val items = KtorClient.apiService.getGroupItems(accessToken, groupId)
                _uiState.value = _uiState.value.copy(items = items, isLoading = false)

                // Отмечаем непросмотренные товары как просмотренные
                val unseenItemIds = items.filter { !it.isViewed }.map { it.id }
                if (unseenItemIds.isNotEmpty()) {
                    KtorClient.apiService.markItemsAsViewed(accessToken, groupId, unseenItemIds)
                    val updatedItems = items.map { item ->
                        if (item.id in unseenItemIds) item.copy(isViewed = true) else item
                    }
                    _uiState.value = _uiState.value.copy(items = updatedItems)
                }
            } catch (e: Exception) {
                handleError(e, "Ошибка загрузки товаров")
            }
        }
    }

    fun addItem(itemId: Int, quantity: Int, priority: Int, budget: Int?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val accessToken = getValidAccessToken()
                KtorClient.apiService.addItemToGroup(accessToken, groupId, itemId, quantity, priority, budget)
                loadItems() // Обновляем список с сервера
            } catch (e: Exception) {
                handleError(e, "Ошибка добавления товара")
            }
        }
    }

    fun buyItem(itemId: Int, price: Int) { // Добавляем параметр price
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val accessToken = getValidAccessToken()
                KtorClient.apiService.buyItem(accessToken, groupId, itemId, price)
                loadItems()
            } catch (e: Exception) {
                handleError(e, "Ошибка покупки товара")
            }
        }
    }

    private suspend fun getValidAccessToken(): String {
        var accessToken = authManager.getAccessToken() ?: throw Exception("Токен отсутствует")
        try {
            // Проверяем токен вызовом API (например, getGroupItems)
            KtorClient.apiService.getGroupItems(accessToken, groupId)
            return accessToken
        } catch (e: Exception) {
            if (e.message == "Токен недействителен") {
                val refreshToken = authManager.getRefreshToken() ?: throw Exception("Refresh-токен отсутствует")
                val response = KtorClient.apiService.refreshToken(refreshToken)
                authManager.saveAuthData(response.token, response.refreshToken, response.user.id)
                return authManager.getAccessToken() ?: throw Exception("Не удалось обновить токен")
            } else {
                throw e
            }
        }
    }

    private fun handleError(e: Exception, defaultMessage: String) {
        if (e.message == "Токен недействителен" || e.message == "Refresh-токен отсутствует") {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Ошибка авторизации: ${e.message}"
            )
        } else {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = e.message ?: defaultMessage
            )
        }
    }
}
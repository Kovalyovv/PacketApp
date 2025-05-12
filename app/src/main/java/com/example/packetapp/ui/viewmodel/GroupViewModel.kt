package com.example.packetapp.ui.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.packetapp.data.AuthManager
import com.example.packetapp.models.GroupListItem
import com.example.packetapp.models.Item
import com.example.packetapp.network.KtorClient
import com.example.packetapp.models.BuyItemRequest
import kotlinx.coroutines.launch

data class GroupUiState(
    val items: List<GroupListItem> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<Item> = emptyList(),
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
        markAllActivitiesAsViewed()
    }

    fun loadItems() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val accessToken = getValidAccessToken()
                val items = KtorClient.apiService.getGroupItems(accessToken, groupId)
                _uiState.value = _uiState.value.copy(items = items, isLoading = false)
            } catch (e: Exception) {
                handleError(e, "Ошибка загрузки товаров")
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        if (query.isNotEmpty()) {
            searchItems(query)
        } else {
            _uiState.value = _uiState.value.copy(searchResults = emptyList())
        }
    }

    fun clearSearch() {
        _uiState.value = _uiState.value.copy(searchQuery = "", searchResults = emptyList())
    }

    private fun searchItems(query: String) {
        viewModelScope.launch {
            try {
                val accessToken = getValidAccessToken()
                val results = KtorClient.apiService.searchItems(accessToken, query)
                _uiState.value = _uiState.value.copy(searchResults = results)
            } catch (e: Exception) {
                handleError(e, "Ошибка поиска товаров")
            }
        }
    }

    fun addItem(itemId: Int, quantity: Int, priority: Int, budget: Int?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val accessToken = getValidAccessToken()
                KtorClient.apiService.addItemToGroup(accessToken, groupId, itemId, quantity, priority, budget)
                loadItems()
                clearSearch() // Очищаем поиск после добавления
            } catch (e: Exception) {
                handleError(e, "Ошибка добавления товара")
            }
        }
    }

    fun buyItem(itemId: Int, quantityBought: Int, price: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val accessToken = getValidAccessToken()
                val userId = authManager.getUserId() ?: throw IllegalStateException("User not logged in")
                val request = BuyItemRequest(
                    groupId = groupId,
                    boughtBy = userId,
                    price = price,
                    quantity = quantityBought
                )
                KtorClient.apiService.buyItem(accessToken, groupId, itemId, request)
                loadItems()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to mark item as bought",
                    isLoading = false
                )
            }
        }
    }

    private fun markAllActivitiesAsViewed() {
        viewModelScope.launch {
            try {
                val accessToken = authManager.getAccessToken() ?: throw Exception("Токен отсутствует")
                KtorClient.apiService.markAllActivitiesAsViewed(accessToken, groupId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Ошибка отметки просмотра: ${e.message}"
                )
            }
        }
    }

    private suspend fun getValidAccessToken(): String {
        var accessToken = authManager.getAccessToken() ?: throw Exception("Токен отсутствует")
        try {
            KtorClient.apiService.getGroupItems(accessToken, groupId)
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
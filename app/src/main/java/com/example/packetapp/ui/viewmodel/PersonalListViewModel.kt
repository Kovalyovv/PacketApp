package com.example.packetapp.ui.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.packetapp.data.AuthManager
import com.example.packetapp.models.Item
import com.example.packetapp.models.PersonalListItem
import com.example.packetapp.models.PurchaseHistoryItem
import com.example.packetapp.network.KtorClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


data class PersonalListUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val personalList: List<PersonalListItem> = emptyList(),
    val purchaseHistory: List<PurchaseHistoryItem> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<Item> = emptyList(),
    val isSearchActive: Boolean = false,
    val showHistory: Boolean = false
)

class PersonalListViewModel(private val authManager: AuthManager) : ViewModel() {
    private val _uiState = mutableStateOf(PersonalListUiState())
    val uiState: State<PersonalListUiState> = _uiState

    private var searchJob: Job? = null

    init {
        loadPersonalList()
    }

    fun loadPersonalList() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val accessToken = authManager.getAccessToken() ?: throw Exception("Токен отсутствует")
                val personalList = KtorClient.apiService.getPersonalList(accessToken)
                _uiState.value = _uiState.value.copy(personalList = personalList, isLoading = false)
            } catch (e: Exception) {
                if (e.message == "Токен недействителен") {
                    try {
                        val refreshToken = authManager.getRefreshToken() ?: throw Exception("Refresh-токен отсутствует")
                        val response = KtorClient.apiService.refreshToken(refreshToken)
                        authManager.saveAuthData(response.token, response.refreshToken, response.user.id, response.user.name, response.user.email)
                        val newAccessToken = authManager.getAccessToken()!!
                        val personalList = KtorClient.apiService.getPersonalList(newAccessToken)
                        _uiState.value = _uiState.value.copy(personalList = personalList, isLoading = false)
                    } catch (refreshError: Exception) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Ошибка авторизации: ${refreshError.message}"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Ошибка загрузки списка"
                    )
                }
            }
        }
    }

    fun loadPurchaseHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val accessToken = authManager.getAccessToken() ?: throw Exception("Токен отсутствует")
                val history = KtorClient.apiService.getPurchaseHistory(accessToken)
                _uiState.value = _uiState.value.copy(purchaseHistory = history, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Ошибка загрузки истории"
                )
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query, isSearchActive = query.isNotEmpty())

        searchJob?.cancel()
        if (query.isEmpty()) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList(), isSearchActive = false)
            return
        }

        searchJob = viewModelScope.launch {
            delay(300)
            searchItems(query)
        }
    }

    private fun searchItems(query: String) {
        viewModelScope.launch {
            try {
                val accessToken = authManager.getAccessToken() ?: throw Exception("Токен отсутствует")
                val results = KtorClient.apiService.searchItems(accessToken, query)
                _uiState.value = _uiState.value.copy(searchResults = results)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Ошибка поиска: ${e.message}"
                )
            }
        }
    }

    fun addItemToPersonalList(itemId: Int, quantity: Int) {
        viewModelScope.launch {
            try {
                val accessToken = authManager.getAccessToken() ?: throw Exception("Токен отсутствует")
                val newItem = KtorClient.apiService.addItemToPersonalList(accessToken, itemId, quantity)
                val updatedList = _uiState.value.personalList + newItem
                _uiState.value = _uiState.value.copy(
                    personalList = updatedList,
                    searchQuery = "",
                    searchResults = emptyList(),
                    isSearchActive = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Ошибка добавления товара: ${e.message}"
                )
            }
        }
    }

    fun markAsPurchased(itemId: Int, price: Int) {
        viewModelScope.launch {
            try {
                val accessToken = authManager.getAccessToken() ?: throw Exception("Токен отсутствует")
                KtorClient.apiService.markAsPurchased(accessToken, itemId, price)
                val updatedList = _uiState.value.personalList.filter { it.id != itemId }
                _uiState.value = _uiState.value.copy(personalList = updatedList)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Ошибка пометки товара: ${e.message}"
                )
            }
        }
    }

    fun toggleShowHistory() {
        val showHistory = !_uiState.value.showHistory
        _uiState.value = _uiState.value.copy(showHistory = showHistory)
        if (showHistory) {
            loadPurchaseHistory()
        }
    }

    fun clearSearch() {
        _uiState.value = _uiState.value.copy(
            searchQuery = "",
            searchResults = emptyList(),
            isSearchActive = false
        )
    }
}
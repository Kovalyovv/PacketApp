package com.example.packetapp.ui.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.packetapp.data.AuthManager
import com.example.packetapp.network.ApiService
import com.example.packetapp.network.KtorClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class ProcessedCheckItem(
    val name: String,
    val price: Int,
    val quantity: Double
)

@Serializable
data class ProcessedCheckData(
    val totalSum: Int,
    val items: List<ProcessedCheckItem>
)


@Serializable
data class ScanReceiptResponse(
    val first: ReceiptDTO,
    val second: ProcessedCheckData,
    val message: String? = null
)

data class ReceiptUiState(
    val isLoading: Boolean = false,
    val checkData: ProcessedCheckData? = null,
    val errorMessage: String? = null,
    val message: String? = null
)

class ReceiptViewModel(
    private val authManager: AuthManager
) : ViewModel() {
    private val apiService: ApiService = KtorClient.apiService

    private val _uiState = MutableStateFlow(ReceiptUiState())
    val uiState: StateFlow<ReceiptUiState> = _uiState.asStateFlow()

    private val _selectedItems = mutableStateListOf<ProcessedCheckItem>()
    val selectedItems: SnapshotStateList<ProcessedCheckItem> = _selectedItems

    fun processQrCode(qrCode: String): Result<Unit> {
        return try {
            viewModelScope.launch {
                _uiState.value = ReceiptUiState(isLoading = true)
                val accessToken = authManager.getAccessToken()
                    ?: throw IllegalStateException("Токен отсутствует")
                val response = apiService.getCheckDataFromServer(accessToken, qrCode)
                _uiState.value = ReceiptUiState(
                    isLoading = false,
                    checkData = response.second,
                    message = response.message
                )
                println("Updated uiState in processQrCode: ${_uiState.value}")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            _uiState.value = ReceiptUiState(
                isLoading = false,
                errorMessage = e.message ?: "Ошибка обработки QR-кода"
            )
            Result.failure(e)
        }
    }

    fun loadReceiptData(qrCode: String) {
        viewModelScope.launch {
            _uiState.value = ReceiptUiState(isLoading = true)
            try {
                val accessToken = authManager.getAccessToken()
                    ?: throw IllegalStateException("Токен отсутствует")
                val response = apiService.getCheckDataFromServer(accessToken, qrCode)
                _uiState.value = ReceiptUiState(
                    isLoading = false,
                    checkData = response.second,
                    message = response.message
                )
                println("Updated uiState in loadReceiptData: ${_uiState.value}")
            } catch (e: Exception) {
                _uiState.value = ReceiptUiState(
                    isLoading = false,
                    errorMessage = e.message ?: "Ошибка загрузки данных чека"
                )
            }
        }
    }

    fun confirmItems(items: List<ProcessedCheckItem>, groupId: Int?) {
        viewModelScope.launch {
            try {
                val accessToken = authManager.getAccessToken()
                    ?: throw IllegalStateException("Токен отсутствует")
                val userId = authManager.getUserId()
                    ?: throw IllegalStateException("Пользователь не авторизован")
                val receiptId = _uiState.value.checkData?.let { checkData ->
                    apiService.getReceiptsHistory(accessToken, userId).find {
                        it.second == checkData
                    }?.first?.id
                } ?: throw IllegalStateException("Чек не найден")
                apiService.confirmItems(accessToken, receiptId, items, groupId)
                if (groupId != null) {
                    apiService.addItemsToGroupActivity(accessToken, groupId, items)
                } else {
                    apiService.addItemsToPersonalList(accessToken, items)
                }
            } catch (e: Exception) {
                Log.e("ReceiptViewModel", "Error confirming items: ${e.message}", e)
                throw e
            }
        }
    }

    fun clearState() {
        _uiState.value = ReceiptUiState()
        _selectedItems.clear()
    }
}

@Serializable
data class ReceiptDTO(
    val id: Int = 0,
    val userId: Int,
    val groupId: Int?,
    val qrCode: String,
    val totalAmount: Int,
    val scannedAt: String
)
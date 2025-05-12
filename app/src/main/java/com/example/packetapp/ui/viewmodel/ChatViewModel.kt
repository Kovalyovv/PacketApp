package com.example.packetapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.packetapp.data.AuthManager
import com.example.packetapp.models.ChatMessage
import com.example.packetapp.network.ApiService
import com.example.packetapp.network.ChatClient
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    private val authManager: AuthManager,
    private val chatClient: ChatClient = ChatClient(authManager = authManager),
    private val apiService: ApiService // Добавляем ApiService
) : ViewModel() {

    val messages: StateFlow<List<ChatMessage>> = chatClient.messages

    // Кэш для имен пользователей
    private val userNamesCache = mutableMapOf<Int, String>()

    init {
        println("ChatViewModel: Initialized")
    }

    suspend fun connectToChat(groupId: Int) {
        try {
            println("ChatViewModel: Loading chat history for group $groupId")
            chatClient.loadChatHistory(groupId)
            println("ChatViewModel: Connecting to WebSocket for group $groupId")
            chatClient.connectToChat(groupId) { message ->
                println("ChatViewModel: Received new message: $message")
                viewModelScope.launch {
                    preloadUserNames(groupId)
                } // Предварительная загрузка имен при получении нового сообщения
            }
        } catch (e: Exception) {
            println("ChatViewModel: Failed to connect to chat: ${e.message}")
        }
    }

    fun sendMessage(message: ChatMessage, replyToToken: String? = null) {
        viewModelScope.launch {
            try {
                if (!chatClient.isConnected) {
                    println("ChatViewModel: WebSocket not connected, reconnecting...")
                    connectToChat(message.groupId)
                }
                val messageToSend = message.copy(replyToToken = replyToToken)
                chatClient.updateMessages(messageToSend)
                chatClient.sendMessage(messageToSend)
                println("ChatViewModel: Sent message with token: ${message.token}")
            } catch (e: Exception) {
                println("ChatViewModel: Failed to send message: ${e.message}")
            }
        }
    }

    fun deleteMessage(token: String) {
        viewModelScope.launch {
            try {
                chatClient.deleteMessage(token)
            } catch (e: Exception) {
                println("ChatViewModel: Failed to delete message: ${e.message}")
            }
        }
    }

    // Получение имени пользователя по ID
    suspend fun getUserName(userId: Int): String {
        return userNamesCache[userId] ?: run {
            val name = apiService.getUserNameById(authManager.getAccessToken() ?: "", userId)
            userNamesCache[userId] = name
            name
        }
    }

    // Предварительная загрузка имен всех участников чата
    suspend fun preloadUserNames(groupId: Int) {
        val accessToken = authManager.getAccessToken() ?: return
        val userIds = try {
            apiService.getChatUserNames(accessToken, groupId).keys.toList()
        } catch (e: Exception) {
            println("ChatViewModel: Failed to load chat user names: ${e.message}")
            emptyList()
        }
        userIds.forEach { userId ->
            if (!userNamesCache.containsKey(userId)) {
                try {
                    val name = getUserName(userId)
                    userNamesCache[userId] = name
                } catch (e: Exception) {
                    println("ChatViewModel: Failed to preload name for user $userId: ${e.message}")
                    userNamesCache[userId] = "Пользователь $userId"
                }
            }
        }
    }

    override fun onCleared() {
        viewModelScope.launch {
            chatClient.disconnect()
            println("ChatViewModel: Disconnected from WebSocket")
        }
        super.onCleared()
    }
}
package com.example.packetapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.packetapp.data.AuthManager
import com.example.packetapp.models.ChatMessage
import com.example.packetapp.network.ChatClient
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    private val authManager: AuthManager,
    private val chatClient: ChatClient = ChatClient(authManager = authManager)
) : ViewModel() {

    val messages: StateFlow<List<ChatMessage>> = chatClient.messages

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
            }
        } catch (e: Exception) {
            println("ChatViewModel: Failed to connect to chat: ${e.message}")
        }
    }

    fun sendMessage(message: ChatMessage, replyToId: Int? = null) {
        viewModelScope.launch {
            try {
                if (!chatClient.isConnected) {
                    println("ChatViewModel: WebSocket not connected, reconnecting...")
                    connectToChat(message.groupId)
                }
                val messageToSend = message.copy(replyToId = replyToId)
                chatClient.updateMessages(messageToSend)
                chatClient.sendMessage(messageToSend)
                println("ChatViewModel: Sent message with id: ${message.id}")
            } catch (e: Exception) {
                println("ChatViewModel: Failed to send message: ${e.message}")
            }
        }
    }

    suspend fun deleteMessage(messageId: Int) {
        try {
            chatClient.deleteMessage(messageId)
        } catch (e: Exception) {
            println("ChatViewModel: Failed to delete message: ${e.message}")
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
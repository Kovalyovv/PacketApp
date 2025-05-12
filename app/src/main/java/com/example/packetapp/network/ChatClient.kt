package com.example.packetapp.network

import com.example.packetapp.data.AuthManager
import com.example.packetapp.models.ChatMessage
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import java.util.UUID

class ChatClient(
    val client: HttpClient = KtorClient.httpClient,
    val authManager: AuthManager
) {
    private val baseUrl = "http://192.168.31.137:8080"
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()
    private var currentSession: DefaultClientWebSocketSession? = null
    var isConnected = false
    private val messageStatuses = mutableMapOf<String, String>()

    suspend fun loadChatHistory(groupId: Int): List<ChatMessage> {
        val accessToken = authManager.getAccessToken() ?: throw Exception("Токен отсутствует")
        val response = client.get("$baseUrl/chat/$groupId/messages") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
        println("ChatClient: Raw JSON response: ${response.body<String>()}")
        val history = response.body<List<ChatMessage>>()
        history.forEach { message ->
            println("ChatClient: Loaded message: $message")
        }
        _messages.value = history
        println("ChatClient: Loaded history with ${history.size} messages")
        return history
    }

    suspend fun connectToChat(groupId: Int, onMessageReceived: (ChatMessage) -> Unit) {
        val accessToken = authManager.getAccessToken() ?: throw Exception("Токен отсутствует")
        try {
            client.webSocket(
                method = HttpMethod.Get,
                host = "192.168.31.137",
                port = 8080,
                path = "/chat/$groupId",
                request = {
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                }
            ) {
                currentSession = this
                isConnected = true
                println("ChatClient: WebSocket connected for group $groupId")

                try {
                    for (frame in incoming) {
                        when (frame) {
                            is Frame.Text -> {
                                val message = receiveDeserialized<ChatMessage>()
                                println("ChatClient: Received WebSocket message with timestamp: ${message.timestamp}")
                                onMessageReceived(message)
                                _messages.update { currentMessages ->
                                    if (currentMessages.any { it.token == message.token }) {
                                        println("ChatClient: Message with token ${message.token} already exists, skipping")
                                        currentMessages
                                    } else {
                                        println("ChatClient: Adding new message to list: $message")
                                        currentMessages + message
                                    }

                                }

                            }
                            is Frame.Close -> {
                                println("ChatClient: WebSocket closed: ${frame.readReason()}")
                                break
                            }
                            else -> println("ChatClient: Received unsupported frame: $frame")
                        }
                    }
                } catch (e: Exception) {
                    println("ChatClient: WebSocket error: ${e.message}")
                } finally {
                    isConnected = false
                    currentSession = null
                    println("ChatClient: WebSocket disconnected for group $groupId")
                }
            }
        } catch (e: Exception) {
            isConnected = false
            currentSession = null
            println("ChatClient: Failed to connect to WebSocket: ${e.message}")
        }
    }

    suspend fun sendMessage(message: ChatMessage) {
        if (!isConnected || currentSession == null) {
            throw IllegalStateException("WebSocket is not connected")
        }
        try {
            messageStatuses[message.token] = "SENDING"
            currentSession?.sendSerialized(message)
            println("ChatClient: Sent message: $message")
        } catch (e: Exception) {
            messageStatuses[message.token] = "FAILED"
            println("ChatClient: Failed to send message: ${e.message}")
        }
    }

    suspend fun deleteMessage(token: String) {
        val accessToken = authManager.getAccessToken() ?: throw Exception("Токен отсутствует")
        try {
            client.delete("$baseUrl/chat/$token") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
            }
            _messages.update { messages -> messages.filter { it.token != token } }
            println("ChatClient: Message deleted: $token")
        } catch (e: Exception) {
            println("ChatClient: Failed to delete message: ${e.message}")
        }
    }

    fun updateMessages(message: ChatMessage) {
        _messages.update { it + message }
        println("ChatClient: Manually updated messages with: $message")
    }

    fun removeMessage(token: String) {
        _messages.update { messages -> messages.filter { it.token != token } }
        println("ChatClient: Removed message with token: $token")
    }

    suspend fun disconnect() {
        if (isConnected && currentSession != null) {
            currentSession?.close(CloseReason(CloseReason.Codes.NORMAL, "Client disconnected"))
            currentSession = null
            isConnected = false
            println("ChatClient: WebSocket manually disconnected")
        }
    }
}
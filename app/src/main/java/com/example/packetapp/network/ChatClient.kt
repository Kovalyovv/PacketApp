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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class ChatClient(
    val client: HttpClient = KtorClient.httpClient,
    val authManager: AuthManager
) {
    private val baseUrl = "http://192.168.31.137:8080"
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()
    private var currentSession: DefaultClientWebSocketSession? = null
    var isConnected = false
    private val messageStatuses = mutableMapOf<Int, String>()
    private val tempToPermanentId = mutableMapOf<Int, Int>() // Map temporary to permanent IDs

    suspend fun loadChatHistory(groupId: Int): List<ChatMessage> {
        val accessToken = authManager.getAccessToken() ?: throw Exception("Токен отсутствует")
        val response = client.get("$baseUrl/chat/$groupId/messages") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
        println("ChatClient: ChatMessage class: ${ChatMessage::class.java}")
        println("ChatClient: ChatMessage fields: ${ChatMessage::class.java.declaredFields.joinToString()}")
        println("ChatClient: ChatMessage methods: ${ChatMessage::class.java.declaredMethods.joinToString()}")

        val rawJson = response.body<String>()
        println("ChatClient: Raw JSON response: $rawJson")
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
                                    if (message.id > 0 && currentMessages.any { it.id == message.id }) {
                                        println("ChatClient: Message with id ${message.id} already exists, skipping")
                                        currentMessages
                                    } else {
                                        val tempMessageIndex = currentMessages.indexOfFirst {
                                            it.id < 0 && it.text == message.text && it.senderId == message.senderId && it.timestamp == message.timestamp
                                        }
                                        val updatedMessages = if (tempMessageIndex != -1) {
                                            // Store temporary-to-permanent ID mapping
                                            val tempId = currentMessages[tempMessageIndex].id
                                            tempToPermanentId[tempId] = message.id
                                            println("ChatClient: Mapped tempId=$tempId to permanentId=${message.id}")

                                            // Replace temporary message
                                            messageStatuses[tempId] = "SENT"
                                            val newList = currentMessages.toMutableList().apply {
                                                set(tempMessageIndex, message)
                                            }

                                            // Update replyToId for messages referencing the temporary ID
                                            newList.map { msg ->
                                                if (msg.replyToId != null && msg.replyToId == tempId) {
                                                    msg.copy(replyToId = message.id)
                                                } else {
                                                    msg
                                                }
                                            }
                                        } else if (message.id > 0) {
                                            println("ChatClient: Adding new message to list: $message")
                                            currentMessages + message
                                        } else {
                                            currentMessages // Ignore temporary messages from server
                                        }
                                        updatedMessages
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
        val mappedMessage = message.copy(
            replyToId = message.replyToId?.let { tempToPermanentId[it] ?: it }
        )
        try {
            // Map replyToId to permanent ID if available

            messageStatuses[mappedMessage.id] = "SENDING"
            currentSession?.sendSerialized(mappedMessage)
            println("ChatClient: Sent message: $mappedMessage")
        } catch (e: Exception) {
            messageStatuses[mappedMessage.id] = "FAILED"
            println("ChatClient: Failed to send message: ${e.message}")
        }
    }

    suspend fun deleteMessage(messageId: Int) {
        val accessToken = authManager.getAccessToken() ?: throw Exception("Токен отсутствует")
        try {
            client.delete("$baseUrl/messages/$messageId") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
            }
            _messages.update { messages -> messages.filter { it.id != messageId } }
            println("ChatClient: Message deleted: $messageId")
        } catch (e: Exception) {
            println("ChatClient: Failed to delete message: ${e.message}")
        }
    }

    fun updateMessages(message: ChatMessage) {
        // Map replyToId to permanent ID if available
        val mappedMessage = message.copy(
            replyToId = message.replyToId?.let { tempToPermanentId[it] ?: it }
        )
        _messages.update { it + mappedMessage }
        println("ChatClient: Manually updated messages with: $mappedMessage")
    }

    fun removeMessage(messageId: Int) {
        _messages.update { messages -> messages.filter { it.id != messageId } }
        println("ChatClient: Removed message with id: $messageId")
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
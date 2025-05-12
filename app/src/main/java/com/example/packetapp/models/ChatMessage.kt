package com.example.packetapp.models

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val token: String, // Уникальный токен (UUID)
    val groupId: Int,
    val senderId: Int,
    val text: String,
    val timestamp: String,
    val replyToToken: String? = null // Токен сообщения, на которое отвечают
)

data class ChatMessageUi(
    val token: String,
    val groupId: Int,
    val senderId: Int,
    val text: String,
    val timestamp: String,
    val replyToToken: String? = null,
    val status: MessageStatus = MessageStatus.SENT // только для UI
)
package com.example.packetapp.models

import kotlinx.serialization.Serializable

@Serializable
data class Activity(
    val id: Int,
    val groupId: Int,
    val userId: Int,
    val userName: String,
    val type: String, // "ADDED" или "BOUGHT"
    val itemId: Int,
    val itemName: String,
    val quantity: Int,
    val isViewed: Boolean,
    val createdAt: String
)

@Serializable
data class Group(
    val id: Int,
    val name: String,
    val members: List<Int>
)

@Serializable
data class GroupSummary(
    val groupId: Int,
    val groupName: String,
    val lastActivity: GroupActivity?,
    val unseenCount: Int
)

@Serializable
data class GroupActivity(
    val type: String,
    val userName: String,
    val itemName: String,
    val itemId: Int // Добавляем itemId
)

@Serializable
data class GroupListItem(
    val id: Int,
    val groupId: Int,
    val itemId: Int,
    val itemName: String,
    val quantity: Int,
    val priority: Int,
    val budget: Int?,

)
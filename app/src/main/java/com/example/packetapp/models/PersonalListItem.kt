package com.example.packetapp.models

import kotlinx.serialization.Serializable

@Serializable
data class PersonalListItem(
    val id: Int,
    val itemId: Int,
    val itemName: String,
    val quantity: Int,
    val addedAt: String
)
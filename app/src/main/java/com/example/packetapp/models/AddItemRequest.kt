package com.example.packetapp.models

import kotlinx.serialization.Serializable

@Serializable
data class AddItemRequest(
    val itemId: Int?,
    val itemName: String,
    val quantity: Int,
    val price: Int
)
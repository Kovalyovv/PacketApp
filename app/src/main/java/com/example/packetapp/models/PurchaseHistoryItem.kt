package com.example.packetapp.models

import kotlinx.serialization.Serializable

@Serializable
data class PurchaseHistoryItem(
    val id: Int,
    val itemId: Int,
    val itemName: String,
    val quantity: Int,
    val price: Int,
    val purchasedAt: String
)
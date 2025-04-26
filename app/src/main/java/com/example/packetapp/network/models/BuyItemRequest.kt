package com.example.packetapp.network.models

import kotlinx.serialization.Serializable

@Serializable
data class BuyItemRequest (
    val groupId: Int,
    val boughtBy:Int,
    val price: Int,
    val quantity: Int
)
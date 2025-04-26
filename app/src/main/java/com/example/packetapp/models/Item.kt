package com.example.packetapp.models

import kotlinx.serialization.Serializable

@Serializable
data class Item(
    val id: Int,
    val name: String,
    val barcode: String?,
    val category: String?,
    val price: Int
)

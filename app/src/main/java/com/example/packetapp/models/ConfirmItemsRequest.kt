package com.example.packetapp.models

import com.example.packetapp.ui.viewmodel.ProcessedCheckItem
import kotlinx.serialization.Serializable

@Serializable
data class ConfirmItemsRequest(
    val receiptId: Int,
    val items: List<ProcessedCheckItem>,
    val groupId: Int? = null
)
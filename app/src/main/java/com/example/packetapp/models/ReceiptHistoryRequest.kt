package com.example.packetapp.models

import com.example.packetapp.ui.viewmodel.ProcessedCheckData
import com.example.packetapp.ui.viewmodel.ProcessedCheckItem
import com.example.packetapp.ui.viewmodel.ReceiptDTO
import kotlinx.serialization.Serializable

@Serializable
data class ReceiptHistoryRequest(
    val receiptId: Int,
    val items: List<ProcessedCheckItem>
)
@Serializable
data class ReceiptHistoryResponse(
    val receipts: List<Pair<ReceiptDTO, ProcessedCheckData>>
)
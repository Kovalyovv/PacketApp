package com.example.packetapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.packetapp.models.GroupListItem

@Composable
fun GroupListItem(item: GroupListItem, onBuy: () -> Unit, isHighlighted: Boolean = false) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isHighlighted -> Color(0xFFFFE082)
                item.isViewed -> Color.White
                else -> Color(0xFFCCFFCC)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.itemName ?: "Без названия", fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Количество: ${item.quantity}", fontSize = 14.sp)
                item.budget?.let {
                    Text(text = "Бюджет: $it", fontSize = 14.sp)
                }
                Text(text = "Приоритет: ${item.priority}", fontSize = 14.sp)
            }
            Button(onClick = onBuy) {
                Text("Куплено")
            }
        }
    }
}
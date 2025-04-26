package com.example.packetapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.packetapp.models.GroupListItem

//@Composable
//fun GroupListItem(
//    item: GroupListItem,
//    onBuy: () -> Unit,
//    isHighlighted: Boolean
//) {
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(vertical = 4.dp)
//            .background(if (isHighlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface),
//        elevation = CardDefaults.cardElevation(4.dp)
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(16.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Column(modifier = Modifier.weight(1f)) {
//                Text(text = "Товар: ${item.itemName}", fontSize = 16.sp)
//                Text(text = "Количество: ${item.quantity}", fontSize = 14.sp)
//                Text(text = "Приоритет: ${item.priority}", fontSize = 14.sp)
//                item.budget?.let {
//                    Text(text = "Бюджет: $it руб.", fontSize = 14.sp)
//                }
//            }
//            Button(onClick = onBuy) {
//                Text("Купить")
//            }
//        }
//    }
//}
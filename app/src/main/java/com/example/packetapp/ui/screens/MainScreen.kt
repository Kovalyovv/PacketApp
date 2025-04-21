package com.example.packetapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.packetapp.data.AuthManager
import com.example.packetapp.models.GroupSummary
import com.example.packetapp.ui.viewmodel.MainUiState
import com.example.packetapp.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLogout: () -> Unit,
    onNavigateToGroup: (Int, Int?) -> Unit // Изменяем сигнатуру: добавляем highlightItemId
) {
    val authManager = AuthManager(LocalContext.current)
    val viewModel = remember { MainViewModel(authManager) }
    val uiState by viewModel.uiState

    println("MainScreen mounted")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мои группы") },
                actions = {
                    TextButton(onClick = {
                        println("Logout clicked")
                        onLogout()
                    }) {
                        Text("Выход")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (uiState.groupSummaries.isEmpty()) {
                Text(
                    text = "Вы не состоите в группах",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    fontSize = 18.sp
                )
            } else {
                LazyColumn {
                    items(uiState.groupSummaries) { summary ->
                        GroupSummaryItem(
                            summary = summary,
                            onClick = { onNavigateToGroup(summary.groupId, null) }, // Обычный переход
                            onActivityClick = { itemId ->
                                onNavigateToGroup(summary.groupId, itemId) // Переход с подсветкой
                            }
                        )
                    }
                }
            }

            uiState.errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
fun GroupSummaryItem(
    summary: GroupSummary,
    onClick: () -> Unit,
    onActivityClick: (Int) -> Unit // Callback для клика по активности
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = summary.groupName, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(4.dp))
                if (summary.lastActivity != null) {
                    val message = when (summary.lastActivity.type) {
                        "ADDED" -> "${summary.lastActivity.userName} добавил(а) товар: ${summary.lastActivity.itemName}"
                        "BOUGHT" -> "${summary.lastActivity.userName} купил(а) товар: ${summary.lastActivity.itemName}"
                        else -> "Неизвестная активность"
                    }
                    Text(
                        text = message,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { onActivityClick(summary.lastActivity.itemId) }
                    )
                } else {
                    Text(text = "Нет активностей", fontSize = 14.sp)
                }
            }
            if (summary.unseenCount > 0) {
                Badge(
                    containerColor = Color.Red,
                    contentColor = Color.White
                ) {
                    Text(text = "+${summary.unseenCount}", modifier = Modifier.padding(4.dp))
                }
            }
        }
    }
}
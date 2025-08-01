package com.example.packetapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.packetapp.data.AuthManager
import com.example.packetapp.models.GroupSummary
import com.example.packetapp.ui.viewmodel.JoinGroupViewModel
import com.example.packetapp.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLogout: () -> Unit,
    onNavigateToGroup: (Int, String, Int?) -> Unit
) {
    val authManager = AuthManager(LocalContext.current)
    val mainViewModel = remember { MainViewModel(authManager) }
    val joinGroupViewModel = remember { JoinGroupViewModel(authManager) }
    val mainUiState by mainViewModel.uiState
    val joinGroupUiState by joinGroupViewModel.uiState

    var showJoinDialog by remember { mutableStateOf(false) }
    var inviteCode by remember { mutableStateOf("") }

    LaunchedEffect(joinGroupUiState.isSuccess) {
        if (joinGroupUiState.isSuccess) {
            mainViewModel.loadGroups()
            joinGroupViewModel.clearState()
        }
    }

    LaunchedEffect(Unit) {
        mainViewModel.loadGroups()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Главная") }
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp) // Отступы по бокам для теней
        ) {
            if (mainUiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (mainUiState.groupSummaries.isEmpty()) {
                Text(
                    text = "Вы не состоите в группах",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    fontSize = 18.sp
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(mainUiState.groupSummaries) { summary ->
                        GroupSummaryItem(
                            summary = summary,
                            onClick = { groupId, groupName -> onNavigateToGroup(groupId, groupName, null) },
                            onActivityClick = { groupId, groupName, itemId ->
                                onNavigateToGroup(groupId, groupName, itemId)
                            }
                        )
                    }
                }
            }

            mainUiState.errorMessage?.let {
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
    onClick: (Int, String) -> Unit,
    onActivityClick: (Int, String, Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .heightIn(min = 100.dp)
            .clickable { onClick(summary.groupId, summary.groupName) },
        elevation = CardDefaults.cardElevation(
            defaultElevation = 12.dp,
            pressedElevation = 16.dp
        ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = summary.groupName, // Уже не null благодаря модели
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (summary.lastActivity != null) {
                    val message = when (summary.lastActivity.type) {
                        "ADDED" -> "${summary.lastActivity.userName} добавил(а) товар: ${summary.lastActivity.itemName}"
                        "BOUGHT" -> "${summary.lastActivity.userName} купил(а) товар: ${summary.lastActivity.itemName}"
                        else -> "Неизвестная активность"
                    }
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable {
                            onActivityClick(summary.groupId, summary.groupName, summary.lastActivity.itemId)
                        }
                    )
                } else {
                    Text(
                        text = "Нет активностей",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (summary.unseenCount > 0) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = Color.White,
                    modifier = Modifier.align(Alignment.Top)
                ) {
                    Text(text = "+${summary.unseenCount}", fontSize = 12.sp, modifier = Modifier.padding(2.dp))
                }
            }
        }
    }
}
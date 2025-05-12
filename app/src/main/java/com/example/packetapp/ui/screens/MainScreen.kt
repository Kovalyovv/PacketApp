package com.example.packetapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.example.packetapp.ui.viewmodel.JoinGroupViewModel
import com.example.packetapp.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLogout: () -> Unit,
    onNavigateToGroup: (Int, Int?) -> Unit
) {
    val authManager = AuthManager(LocalContext.current)
    val mainViewModel = remember { MainViewModel(authManager) }
    val joinGroupViewModel = remember { JoinGroupViewModel(authManager) }
    val mainUiState by mainViewModel.uiState
    val joinGroupUiState by joinGroupViewModel.uiState

    var showJoinDialog by remember { mutableStateOf(false) }
    var inviteCode by remember { mutableStateOf("") }

    // Обновляем список групп после успешного присоединения
    LaunchedEffect(joinGroupUiState.isSuccess) {
        if (joinGroupUiState.isSuccess) {
            mainViewModel.loadGroups()
            joinGroupViewModel.clearState()
        }
    }

    // Загружаем группы при открытии экрана
    LaunchedEffect(Unit) {
        mainViewModel.loadGroups()
    }

//    // Диалоговое окно для ввода инвайт-кода
//    if (showJoinDialog) {
//        AlertDialog(
//            modifier = Modifier
//                .fillMaxWidth(fraction = 0.9f)
//                .wrapContentHeight(),
//            onDismissRequest = {
//                showJoinDialog = false
//                inviteCode = ""
//                joinGroupViewModel.clearState()
//            },
//            title = { Text("Присоединиться к группе") },
//            text = {
//                Column(
//                    modifier = Modifier.fillMaxWidth()
//                ) {
//                    OutlinedTextField(
//                        value = inviteCode,
//                        onValueChange = { inviteCode = it },
//                        label = { Text("Инвайт-код") },
//                        modifier = Modifier.fillMaxWidth()
//                    )
//                    Spacer(modifier = Modifier.height(8.dp))
//                    joinGroupUiState.errorMessage?.let {
//                        Text(
//                            text = it,
//                            color = MaterialTheme.colorScheme.error,
//                            modifier = Modifier.align(Alignment.CenterHorizontally)
//                        )
//                    }
//                }
//            },
//            confirmButton = {
//                Button(
//                    onClick = {
//                        if (inviteCode.isNotBlank()) {
//                            joinGroupViewModel.joinGroup(inviteCode)
//                            showJoinDialog = false
//                            inviteCode = ""
//                            joinGroupViewModel.clearState()
//                        }
//                    },
//
//                ) {
//                    Text("Присоединиться")
//                }
//            },
//            dismissButton = {
//                TextButton(onClick = {
//                    showJoinDialog = false
//                    inviteCode = ""
//                    joinGroupViewModel.clearState()
//                }) {
//                    Text("Отмена")
//                }
//            }
//        )
//    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мои группы") }
            )
        },
//        floatingActionButton = {
//            FloatingActionButton(onClick = { showJoinDialog = true }) {
//                Icon(Icons.Default.Add, contentDescription = "Присоединиться к группе")
//            }
//        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
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
                LazyColumn {
                    items(mainUiState.groupSummaries) { summary ->
                        GroupSummaryItem(
                            summary = summary,
                            onClick = { onNavigateToGroup(summary.groupId, null) },
                            onActivityClick = { itemId ->
                                onNavigateToGroup(summary.groupId, itemId)
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
    onClick: () -> Unit,
    onActivityClick: (Int) -> Unit
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
                .padding(12.dp),
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
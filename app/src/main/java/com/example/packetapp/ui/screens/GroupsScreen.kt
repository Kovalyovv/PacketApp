package com.example.packetapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.packetapp.data.AuthManager
import com.example.packetapp.models.GroupSummary
import com.example.packetapp.network.KtorClient.apiService
import com.example.packetapp.ui.viewmodel.JoinGroupViewModel
import com.example.packetapp.ui.viewmodel.MainViewModel
import com.example.packetapp.ui.viewmodel.ReceiptViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    onNavigateToGroup: (Int, Int?) -> Unit
) {
    val authManager = AuthManager(LocalContext.current)
    val mainViewModel = remember { MainViewModel(authManager) }
    val joinGroupViewModel = remember { JoinGroupViewModel(authManager) }
    val mainUiState by mainViewModel.uiState
    val joinGroupUiState by joinGroupViewModel.uiState
    val receiptViewModel = remember { ReceiptViewModel(authManager) }

    var showJoinDialog by remember { mutableStateOf(false) }
    var inviteCode by remember { mutableStateOf("") }

    // Для отображения Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Обновляем список групп и показываем уведомление после завершения операции
    LaunchedEffect(joinGroupUiState.isSuccess, joinGroupUiState.errorMessage) {
        if (joinGroupUiState.isSuccess) {
            mainViewModel.loadGroups() // Обновляем список групп
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Успешно присоединились к группе!")
            }
            joinGroupViewModel.clearState()
        } else if (joinGroupUiState.errorMessage != null) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(joinGroupUiState.errorMessage!!)
            }
            joinGroupViewModel.clearState()
        }
    }

    // Диалоговое окно для ввода инвайт-кода
    if (showJoinDialog) {
        Dialog(
            onDismissRequest = {
                showJoinDialog = false
                inviteCode = ""
                joinGroupViewModel.clearState()
            }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(fraction = 0.9f)
                    .wrapContentHeight()
                    .padding(0.dp)

            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Присоединиться к группе",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = inviteCode,
                        onValueChange = { inviteCode = it },
                        label = { Text("Инвайт-код") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = {
                                showJoinDialog = false
                                inviteCode = ""
                                joinGroupViewModel.clearState()
                            }
                        ) {
                            Text("Отмена")
                        }

                        Button(
                            onClick = {
                                if (inviteCode.isNotBlank()) {
                                    joinGroupViewModel.joinGroup(inviteCode)
                                    showJoinDialog = false
                                    inviteCode = ""
                                }
                            },
                            enabled = inviteCode.isNotBlank() && !joinGroupUiState.isLoading
                        ) {
                            Text("Присоединиться")
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }, // Используем snackbarHost для Material 3
        topBar = {
            TopAppBar(
                title = { Text("Мои группы") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showJoinDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Присоединиться к группе")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
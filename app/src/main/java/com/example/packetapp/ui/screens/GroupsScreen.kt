package com.example.packetapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
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
    onNavigateToGroup: (Int, String, Int?) -> Unit
) {
    val authManager = AuthManager(LocalContext.current)
    val mainViewModel = remember { MainViewModel(authManager) }
    val joinGroupViewModel = remember { JoinGroupViewModel(authManager) }
    val mainUiState by mainViewModel.uiState
    val joinGroupUiState by joinGroupViewModel.uiState
    val receiptViewModel = remember { ReceiptViewModel(authManager) }

    var showJoinDialog by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var inviteCode by remember { mutableStateOf("") }
    var groupName by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Обновляем список групп только при успехе
    LaunchedEffect(joinGroupUiState.isSuccess) {
        if (joinGroupUiState.isSuccess) {
            mainViewModel.loadGroups()
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Успешно присоединились к группе!")
            }
            joinGroupViewModel.clearState()
        }
    }

    // Показываем Snackbar при ошибке
    LaunchedEffect(joinGroupUiState.errorMessage) {
        if (joinGroupUiState.errorMessage != null && !joinGroupUiState.isLoading) {
            val errorMsg = joinGroupUiState.errorMessage // Захватываем значение
            coroutineScope.launch {
                if (errorMsg != null) {
                    snackbarHostState.showSnackbar(errorMsg)
                } // Используем захваченное значение
            }
            joinGroupViewModel.clearState()
        }
    }

    // Диалоговое окно для создания группы
    if (showCreateDialog) {
        Dialog(
            onDismissRequest = {
                showCreateDialog = false
                groupName = ""
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
                        text = "Создать новую группу",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Название группы") },
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
                                showCreateDialog = false
                                groupName = ""
                            }
                        ) {
                            Text("Отмена")
                        }

                        Button(
                            onClick = {
                                if (groupName.isNotBlank()) {
                                    val userId = authManager.getUserId() ?: return@Button
                                    coroutineScope.launch {
                                        try {
                                            val accessToken = authManager.getAccessToken() ?: return@launch
                                            apiService.createGroup(accessToken, groupName, userId)
                                            mainViewModel.loadGroups()
                                            snackbarHostState.showSnackbar("Группа успешно создана!")
                                        } catch (e: Exception) {
                                            snackbarHostState.showSnackbar("Ошибка создания группы: ${e.message}")
                                        }
                                    }
                                    showCreateDialog = false
                                    groupName = ""
                                }
                            },
                            enabled = groupName.isNotBlank()
                        ) {
                            Text("Создать")
                        }
                    }
                }
            }
        }
    }

    // Диалоговое окно для ввода инвайт-кода
    if (showJoinDialog) {
        Dialog(
            onDismissRequest = { showJoinDialog = false }
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
                        ),
                        enabled = !joinGroupUiState.isLoading
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
                                }
                            },
                            enabled = inviteCode.isNotBlank() && !joinGroupUiState.isLoading
                        ) {
                            Text("Присоединиться")
                        }
                    }

                    if (joinGroupUiState.isLoading) {
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                    joinGroupUiState.errorMessage?.let {
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
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Мои группы") }
            )
        },
        floatingActionButton = {
            Row {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.padding(end = 16.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Create, contentDescription = "Создать группу")
                }
                FloatingActionButton(
                    onClick = { showJoinDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Присоединиться к группе")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(horizontal = 16.dp)
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
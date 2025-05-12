package com.example.packetapp.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import com.example.packetapp.ui.viewmodel.ProcessedCheckItem
import com.example.packetapp.ui.viewmodel.ReceiptViewModel
import com.example.packetapp.network.KtorClient
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class GroupDTO(
    val id: Int,
    val name: String,
    val members: List<Int>,
    val isPersonal: Boolean,
    val createdAt: String,
    val inviteCode: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScreen(
    onBack: () -> Unit,
    context: Context,
    receiptViewModel: ReceiptViewModel
) {
    val uiState by receiptViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val userId = remember {
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        prefs.getInt("userId", -1)
    }

    val groups = remember { mutableStateOf<List<GroupDTO>>(emptyList()) }
    LaunchedEffect(userId) {
        if (userId != -1) {
            try {
                val accessToken = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                    .getString("accessToken", "") ?: ""
                groups.value = KtorClient.apiService.getUserGroups(accessToken, userId)
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Ошибка загрузки групп: ${e.message}")
            }
        }
    }

    var selectedGroup by remember { mutableStateOf<GroupDTO?>(null) }
    var isPersonalListSelected by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    println("ReceiptScreen uiState: $uiState")
    LaunchedEffect(uiState) {
        println("ReceiptScreen uiState updated: $uiState")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Чек") },
                navigationIcon = {
                    IconButton(onClick = {
                        receiptViewModel.clearState()
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.errorMessage != null -> {
                    Text(
                        text = uiState.errorMessage!!,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    println("ReceiptScreen: Displaying error - ${uiState.errorMessage}")
                }
                uiState.checkData != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        uiState.message?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }

                        Text(
                            text = "Общая сумма: ${uiState.checkData!!.totalSum} руб.",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Отключаем выбор группы, если QR-код уже сканирован
                        val isAlreadyScanned = uiState.message == "QR-код уже был отсканирован ранее"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box {
                                OutlinedButton(
                                    onClick = { if (!isAlreadyScanned) expanded = true },
                                    enabled = !isAlreadyScanned // Отключаем кнопку, если уже сканирован
                                ) {
                                    Text(
                                        if (isPersonalListSelected) "Личный список"
                                        else selectedGroup?.name ?: "Выберите группу"
                                    )
                                }
                                DropdownMenu(
                                    expanded = expanded && !isAlreadyScanned, // Отключаем выпадающее меню
                                    onDismissRequest = { expanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Личный список") },
                                        onClick = {
                                            isPersonalListSelected = true
                                            selectedGroup = null
                                            expanded = false
                                        },
                                        enabled = !isAlreadyScanned
                                    )
                                    groups.value.forEach { group ->
                                        DropdownMenuItem(
                                            text = { Text(group.name) },
                                            onClick = {
                                                isPersonalListSelected = false
                                                selectedGroup = group
                                                expanded = false
                                            },
                                            enabled = !isAlreadyScanned
                                        )
                                    }
                                }
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.weight(1f)
                        ) {
                            val items = uiState.checkData?.items ?: emptyList()
                            items(items) { item ->
                                ReceiptItemRow(item)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        val groupId = if (isPersonalListSelected) null else selectedGroup?.id
                                        val items = uiState.checkData?.items ?: emptyList()
                                        receiptViewModel.confirmItems(items, groupId)
                                        snackbarHostState.showSnackbar("Товары успешно подтверждены!")
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Ошибка: ${e.message}")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isAlreadyScanned && // Отключаем кнопку, если уже сканирован
                                    (isPersonalListSelected || selectedGroup != null) &&
                                    (uiState.checkData?.items?.isNotEmpty() == true)
                        ) {
                            Text("Подтвердить товары")
                        }
                    }
                }
                else -> {
                    Text(
                        text = "Нет данных чека",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
fun ReceiptItemRow(item: ProcessedCheckItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f, fill = true)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Количество: ${item.quantity}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = "${item.price} руб.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
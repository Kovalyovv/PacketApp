package com.example.packetapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.packetapp.data.AuthManager
import com.example.packetapp.ui.viewmodel.GroupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupScreen(
    groupId: Int,
    onBack: () -> Unit,
    highlightItemId: Int? = null
) {
    val authManager = AuthManager(LocalContext.current)
    val viewModel = remember { GroupViewModel(authManager, groupId) }
    val uiState by viewModel.uiState

    var showAddDialog by remember { mutableStateOf(false) }
    var showBuyDialog by remember { mutableStateOf(false) }
    var selectedItemId by remember { mutableStateOf<Int?>(null) }
    var itemId by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var priority by remember { mutableStateOf("0") }
    var budget by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Добавить товар") },
            text = {
                Column {
                    OutlinedTextField(
                        value = itemId,
                        onValueChange = { itemId = it },
                        label = { Text("ID товара") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("Количество") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = priority,
                        onValueChange = { priority = it },
                        label = { Text("Приоритет") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = budget,
                        onValueChange = { budget = it },
                        label = { Text("Бюджет (опционально)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val itemIdInt = itemId.toIntOrNull()
                        val quantityInt = quantity.toIntOrNull()
                        val priorityInt = priority.toIntOrNull()
                        val budgetInt = budget.toIntOrNull()
                        if (itemIdInt != null && quantityInt != null && priorityInt != null) {
                            viewModel.addItem(itemIdInt, quantityInt, priorityInt, budgetInt)
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Добавить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showBuyDialog && selectedItemId != null) {
        AlertDialog(
            onDismissRequest = { showBuyDialog = false },
            title = { Text("Купить товар") },
            text = {
                Column {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Цена покупки") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val priceInt = price.toIntOrNull()
                        if (priceInt != null) {
                            viewModel.buyItem(selectedItemId!!, priceInt)
                            showBuyDialog = false
                            selectedItemId = null
                            price = ""
                        }
                    }
                ) {
                    Text("Подтвердить")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBuyDialog = false
                    selectedItemId = null
                    price = ""
                }) {
                    Text("Отмена")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Группа") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить товар")
            }
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
            } else if (uiState.items.isEmpty()) {
                Text(
                    text = "Нет товаров для покупки",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    fontSize = 18.sp
                )
            } else {
                LazyColumn {
                    items(uiState.items) { item ->
                        GroupListItem(
                            item = item,
                            onBuy = {
                                selectedItemId = item.id
                                showBuyDialog = true
                            },
                            isHighlighted = highlightItemId == item.id
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
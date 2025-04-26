package com.example.packetapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.packetapp.data.AuthManager
import com.example.packetapp.models.GroupListItem
import com.example.packetapp.models.Item
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

    var showSearchDialog by remember { mutableStateOf(false) } // Первое окно (поиск)
    var showDetailsDialog by remember { mutableStateOf(false) } // Второе окно (детали)
    var selectedItem by remember { mutableStateOf<Item?>(null) } // Выбранный товар
    var showBuyDialog by remember { mutableStateOf(false) }
    var selectedItemId by remember { mutableStateOf<Int?>(null) }
    var itemId by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var priority by remember { mutableStateOf("0") }
    var budget by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var buyQuantity by remember { mutableStateOf("") }

    // Первое диалоговое окно: Поиск товаров
    if (showSearchDialog) {
        AlertDialog(
            modifier = Modifier
                .fillMaxWidth(fraction = 1f) // 90% ширины экрана
                .fillMaxHeight(fraction = 0.9f), // 80% высоты экрана
            onDismissRequest = {
                showSearchDialog = false
                viewModel.clearSearch()
            },
            title = { Text("Поиск товара") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                ) {
                    // Поле поиска
                    SearchField(
                        query = uiState.searchQuery,
                        onQueryChange = { viewModel.updateSearchQuery(it) },
                        onClear = { viewModel.clearSearch() },
                        isSearchActive = uiState.searchQuery.isNotEmpty()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Результаты поиска
                    if (uiState.searchQuery.isNotEmpty()) {
                        if (uiState.searchResults.isEmpty()) {
                            Text(
                                text = "Товары не найдены",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .wrapContentHeight(align = Alignment.CenterVertically),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontSize = 16.sp
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f) // Занимает всё доступное пространство
                            ) {
                                items(uiState.searchResults) { item ->
                                    SearchResultItem(
                                        item = item,
                                        onSelect = {
                                            selectedItem = item
                                            showSearchDialog = false
                                            showDetailsDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    // Кнопки внизу
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = {
                            showSearchDialog = false
                            viewModel.clearSearch()
                        }) {
                            Text("Отмена")
                        }
                        TextButton(onClick = {
                            selectedItem = null
                            showSearchDialog = false
                            showDetailsDialog = true
                        }) {
                            Text("Добавить вручную")
                        }
                    }
                }
            },
            confirmButton = {} // Убрали confirmButton, так как кнопки теперь в Row
        )
    }

    // Второе диалоговое окно: Указание количества, приоритета и бюджета
    if (showDetailsDialog) {
        // Вычисляем начальное значение budget, если товар выбран
        LaunchedEffect(selectedItem, quantity) {
            if (selectedItem != null) {
                val quantityInt = quantity.toIntOrNull() ?: 1
                budget = (selectedItem!!.price * quantityInt).toString()
            } else {
                budget = ""
            }
        }

        AlertDialog(
            modifier = Modifier
                .fillMaxWidth(fraction = 0.9f)
                .fillMaxHeight(fraction = 0.5f),
            onDismissRequest = {
                showDetailsDialog = false
                selectedItem = null
                itemId = ""
                quantity = "1"
                priority = "0"
                budget = ""
            },
            title = {
                Text(
                    if (selectedItem != null) "Добавить ${selectedItem?.name}"
                    else "Добавить товар вручную"
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                ) {
                    if (selectedItem == null) {
                        OutlinedTextField(
                            value = itemId,
                            onValueChange = { itemId = it },
                            label = { Text("ID товара") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
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
                        label = { Text("Бюджет") }, // Убрали "(опционально)", так как есть дефолтное значение
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.weight(1f))

                    // Кнопки внизу
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = {
                            showDetailsDialog = false
                            selectedItem = null
                            itemId = ""
                            quantity = "1"
                            priority = "0"
                            budget = ""
                        }) {
                            Text("Отмена")
                        }
                        Button(
                            onClick = {
                                val quantityInt = quantity.toIntOrNull()
                                val priorityInt = priority.toIntOrNull()
                                var budgetInt: Int? = budget.toIntOrNull() // Пользовательское значение бюджета

                                // Если бюджет не введён и товар выбран, используем item.price * quantity
                                if (budgetInt == null && selectedItem != null && quantityInt != null) {
                                    budgetInt = selectedItem!!.price * quantityInt
                                }

                                if (quantityInt != null && priorityInt != null) {
                                    if (selectedItem != null) {
                                        viewModel.addItem(selectedItem!!.id, quantityInt, priorityInt, budgetInt)
                                    } else {
                                        val itemIdInt = itemId.toIntOrNull()
                                        if (itemIdInt != null) {
                                            viewModel.addItem(itemIdInt, quantityInt, priorityInt, budgetInt)
                                        }
                                    }
                                    showDetailsDialog = false
                                    selectedItem = null
                                    itemId = ""
                                    quantity = "1"
                                    priority = "0"
                                    budget = ""
                                }
                            },
                            enabled = (selectedItem != null || itemId.toIntOrNull() != null) &&
                                    quantity.toIntOrNull() != null &&
                                    priority.toIntOrNull() != null
                        ) {
                            Text("Добавить")
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Диалог покупки (оставляем без изменений)
    if (showBuyDialog && selectedItemId != null) {
        val selectedItem = uiState.items.find { it.id == selectedItemId }
        LaunchedEffect(selectedItemId) {
            if (selectedItem != null) {
                buyQuantity = selectedItem.quantity.toString()
            }
        }
        AlertDialog(
            onDismissRequest = { showBuyDialog = false },
            title = { Text("Купить товар ${selectedItem?.itemName ?: ""}") },
            text = {
                Column {
                    OutlinedTextField(
                        value = buyQuantity,
                        onValueChange = { buyQuantity = it },
                        label = { Text("Количество (из ${selectedItem?.quantity ?: 0})") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Цена за единицу") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val quantityInt = buyQuantity.toIntOrNull()
                        val priceInt = price.toIntOrNull()
                        if (quantityInt != null && priceInt != null && quantityInt > 0) {
                            viewModel.buyItem(selectedItemId!!, quantityInt, priceInt)
                            showBuyDialog = false
                            selectedItemId = null
                            buyQuantity = ""
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
                    buyQuantity = ""
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
            FloatingActionButton(onClick = { showSearchDialog = true }) {
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



@Composable
fun SearchResultItem(
    item: Item,
    onSelect: () -> Unit // Изменили с onAddClick на onSelect, так как теперь только выбираем
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.name, fontSize = 16.sp)
            item.category?.let {
                Text(text = "Категория: $it", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(text = "Цена: ${item.price} руб.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(onClick = onSelect) {
            Text("Выбрать")
        }
    }
}

@Composable
fun GroupListItem(
    item: GroupListItem,
    onBuy: () -> Unit,
    isHighlighted: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(if (isHighlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Товар: ${item.itemName}", fontSize = 16.sp)
                Text(text = "Количество: ${item.quantity}", fontSize = 14.sp)
                Text(text = "Приоритет: ${item.priority}", fontSize = 14.sp)
                item.budget?.let {
                    Text(text = "Бюджет: $it руб.", fontSize = 14.sp)
                }
            }
            Button(onClick = onBuy) {
                Text("Купить")
            }
        }
    }
}
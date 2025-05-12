package com.example.packetapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.packetapp.R
import com.example.packetapp.data.AuthManager
import com.example.packetapp.models.GroupListItem
import com.example.packetapp.models.Item
import com.example.packetapp.ui.viewmodel.GroupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupScreen(
    groupId: Int,
    onBack: () -> Unit,
    onNavigateToChat: (Int) -> Unit,
    highlightItemId: Int? = null
) {
    val authManager = AuthManager(LocalContext.current)
    val viewModel = remember { GroupViewModel(authManager, groupId) }
    val uiState by viewModel.uiState

    var showSearchDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<Item?>(null) }
    var showBuyDialog by remember { mutableStateOf(false) }
    var selectedItemId by remember { mutableStateOf<Int?>(null) }
    var itemId by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var priority by remember { mutableStateOf("0") }
    var budget by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var buyQuantity by remember { mutableStateOf("") }

    if (showSearchDialog) {
        AlertDialog(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.9f)
                .padding(0.dp),
            properties = DialogProperties(usePlatformDefaultWidth = false),
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
                    SearchField(
                        query = uiState.searchQuery,
                        onQueryChange = { viewModel.updateSearchQuery(it) },
                        onClear = { viewModel.clearSearch() },
                        isSearchActive = uiState.searchQuery.isNotEmpty()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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
                                    .weight(1f)
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
                                    Divider(
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                        thickness = 1.dp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = {
                            showSearchDialog = false
                            viewModel.clearSearch()
                        }) { Text("Отмена") }
                        TextButton(onClick = {
                            selectedItem = null
                            showSearchDialog = false
                            showDetailsDialog = true
                        }) { Text("Добавить вручную") }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showDetailsDialog) {
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
                .fillMaxWidth(fraction = 0.99f)
                .fillMaxHeight(fraction = 0.6f),
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
                        label = { Text("Бюджет") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.weight(1f))
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
                        }) { Text("Отмена") }
                        Button(
                            onClick = {
                                val quantityInt = quantity.toIntOrNull()
                                val priorityInt = priority.toIntOrNull()
                                var budgetInt: Int? = budget.toIntOrNull()
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
                        ) { Text("Добавить") }
                    }
                }
            },
            confirmButton = {}
        )
    }

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
                ) { Text("Подтвердить") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBuyDialog = false
                    selectedItemId = null
                    buyQuantity = ""
                    price = ""
                }) { Text("Отмена") }
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
            Row {
                FloatingActionButton(
                    onClick = { onNavigateToChat(groupId) },
                    modifier = Modifier.padding(end = 16.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(painterResource(id = R.drawable.chat_64d), contentDescription = "Чат группы", tint = MaterialTheme.colorScheme.onPrimary)
                }
                FloatingActionButton(
                    onClick = { showSearchDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить товар", tint = MaterialTheme.colorScheme.onPrimary)
                }
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
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(1.dp), // Убираем возможные внутренние отступы

        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                item.category?.let {
                    Text(
                        text = "Категория: $it",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Цена: ${item.price} руб.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onSelect,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("Выбрать")
            }
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
                .padding(10.dp).padding(horizontal = 6.dp),
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
                Text("Куплено")
            }
        }
    }
}
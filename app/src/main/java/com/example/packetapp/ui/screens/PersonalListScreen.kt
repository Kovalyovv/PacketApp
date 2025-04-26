package com.example.packetapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.packetapp.data.AuthManager
import com.example.packetapp.models.Item
import com.example.packetapp.models.PersonalListItem
import com.example.packetapp.models.PurchaseHistoryItem
import com.example.packetapp.utils.DateUtils
import com.example.packetapp.ui.viewmodel.PersonalListViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalListScreen() {
    val context = LocalContext.current
    val authManager = remember { AuthManager(context) }
    val viewModel = remember { PersonalListViewModel(authManager) }

    val uiState by viewModel.uiState

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.showHistory) "История покупок" else "Личный список") },
                actions = {
                    IconButton(onClick = { viewModel.toggleShowHistory() }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Переключить на историю/список"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!uiState.showHistory) {
                // Поле поиска
                SearchField(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.updateSearchQuery(it) },
                    onClear = { viewModel.clearSearch() },
                    isSearchActive = uiState.isSearchActive
                )

                // Выпадающий список с результатами поиска
                if (uiState.isSearchActive && uiState.searchResults.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 200.dp)
                        ) {
                            items(uiState.searchResults) { item ->
                                SearchResultItem(
                                    item = item,
                                    onAddClick = { quantity ->
                                        viewModel.addItemToPersonalList(item.id, quantity)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Личный список
                if (uiState.isLoading) {
                    CircularProgressIndicator()
                } else if (uiState.personalList.isEmpty()) {
                    Text(
                        text = "Ваш личный список пуст",
                        fontSize = 18.sp
                    )
                } else {
                    LazyColumn {
                        items(uiState.personalList) { item ->
                            PersonalListItemCard(
                                item = item,
                                onMarkPurchased = { price -> viewModel.markAsPurchased(item.id, price) }
                            )
                        }
                    }
                }
            } else {
                // История покупок
                if (uiState.isLoading) {
                    CircularProgressIndicator()
                } else if (uiState.purchaseHistory.isEmpty()) {
                    Text(
                        text = "История покупок пуста",
                        fontSize = 18.sp
                    )
                } else {
                    LazyColumn {
                        items(uiState.purchaseHistory) { item ->
                            PurchaseHistoryItemCard(item = item)
                        }
                    }
                }
            }

            // Сообщение об ошибке
            uiState.errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    isSearchActive: Boolean
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text("Поиск товаров") },
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            if (isSearchActive) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Clear, contentDescription = "Очистить")
                }
            }
        }
    )
}

@Composable
fun SearchResultItem(
    item: Item,
    onAddClick: (quantity: Int) -> Unit
) {
    var quantity by remember { mutableStateOf("1") }
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
        OutlinedTextField(
            value = quantity,
            onValueChange = { quantity = it },
            label = { Text("Кол-во") },
            modifier = Modifier.width(100.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = {
                val qty = quantity.toIntOrNull() ?: 1
                onAddClick(qty)
            }
        ) {
            Text("Добавить")
        }
    }
}

@Composable
fun PersonalListItemCard(
    item: PersonalListItem,
    onMarkPurchased: (price: Int) -> Unit
) {
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Товар: ${item.itemName}", fontSize = 16.sp)
                Text(text = "Количество: ${item.quantity}", fontSize = 14.sp)
                Text(
                    text = "Добавлено: ${DateUtils.formatDateTime(item.addedAt)}",
                    fontSize = 14.sp
                )
            }
            Button(
                onClick = {
                    // Здесь нужно передать цену. Для примера используем заглушку.
                    // В реальном приложении цену нужно получить из Items или передать из SearchResultItem.
                    val price = 100 // Замени на реальную цену товара
                    onMarkPurchased(price)
                }
            ) {
                Text("Куплено")
            }
        }
    }
}

@Composable
fun PurchaseHistoryItemCard(item: PurchaseHistoryItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(text = "Товар: ${item.itemName}", fontSize = 16.sp)
            Text(text = "Количество: ${item.quantity}", fontSize = 14.sp)
            Text(text = "Цена: ${item.price} руб.", fontSize = 14.sp)
            Text(
                text = "Куплено: ${DateUtils.formatDateTime(item.purchasedAt)}",
                fontSize = 14.sp
            )
        }
    }
}
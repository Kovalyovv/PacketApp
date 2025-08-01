package com.example.packetapp.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
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
                .padding(horizontal = 16.dp), // Отступы для теней
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!uiState.showHistory) {
                SearchField(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.updateSearchQuery(it) },
                    onClear = { viewModel.clearSearch() },
                    isSearchActive = uiState.isSearchActive
                )

                if (uiState.isSearchActive && uiState.searchResults.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .heightIn(min = 400.dp, max = 900.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 900.dp),
                            contentPadding = PaddingValues(vertical = 8.dp) // Для теней
                        ) {
                            items(uiState.searchResults) { item ->
                                SearchResultItem(
                                    item = item,
                                    onAddClick = { quantity ->
                                        viewModel.addItemToPersonalList(item.id, item.price, quantity, item.name)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.isLoading) {
                    CircularProgressIndicator()
                } else if (uiState.personalList.isEmpty()) {
                    Text(
                        text = "Ваш личный список пуст",
                        fontSize = 18.sp
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 16.dp), // Для теней
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.personalList) { item ->
                            PersonalListItemCard(
                                item = item,
                                onMarkPurchased = { viewModel.markAsPurchased(item.id) }
                            )
                        }
                    }
                }
            } else {
                if (uiState.isLoading) {
                    CircularProgressIndicator()
                } else if (uiState.purchaseHistory.isEmpty()) {
                    Text(
                        text = "История покупок пуста",
                        fontSize = 18.sp
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 16.dp), // Для теней
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.purchaseHistory) { item ->
                            PurchaseHistoryItemCard(item = item)
                        }
                    }
                }
            }

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
            .padding(8.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(text = item.name, fontSize = 16.sp)
            item.category?.let {
                Text(
                    text = "Категория: $it",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Цена: ${item.price} руб.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CompactQuantityField(
                quantity = quantity,
                onQuantityChange = { quantity = it }
            )
            Spacer(modifier = Modifier.height(8.dp))
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
}

@Composable
fun PersonalListItemCard(
    item: PersonalListItem,
    onMarkPurchased: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .heightIn(min = 100.dp), // Минимальная высота для теней
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f, fill = true)) {
                Text(
                    text = item.itemName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Количество: ${item.quantity}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Цена: ${item.price} руб.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = { onMarkPurchased(item.price) },
                modifier = Modifier
                    .height(40.dp)
                    .padding(start = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Куплено", fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun PurchaseHistoryItemCard(item: PurchaseHistoryItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp) // Увеличены отступы
            .heightIn(min = 90.dp), // Минимальная высота для теней
        elevation = CardDefaults.cardElevation(
            defaultElevation = 12.dp, // Увеличено
            pressedElevation = 16.dp
        ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "${item.itemName}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Количество: ${item.quantity} | Цена: ${item.price} руб.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Куплено: ${DateUtils.formatDateTime(item.purchasedAt)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CompactQuantityField(quantity: String, onQuantityChange: (String) -> Unit) {
    OutlinedTextField(
        value = quantity,
        onValueChange = onQuantityChange,
        label = { Text("Кол-во", fontSize = 12.sp) }, // Уменьшаем размер текста метки

        modifier = Modifier
            .width(80.dp)
            .height(52.dp) // Слегка увеличиваем высоту для лучшей видимости
            .padding(0.dp),
        textStyle = TextStyle(fontSize = 14.sp), // Уменьшаем размер текста ввода
        singleLine = true, // Ограничиваем поле одной строкой
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Number // Ограничиваем ввод только числами
        ),
        colors = TextFieldDefaults.colors(
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}
package com.example.packetapp.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.packetapp.R
import com.example.packetapp.data.AuthManager
import com.example.packetapp.models.GroupListItem
import com.example.packetapp.models.Item
import com.example.packetapp.ui.viewmodel.GroupViewModel
import android.widget.Toast
import androidx.compose.foundation.clickable
import com.example.packetapp.utils.NotificationHelper

fun priorityToCategory(priority: Int): String {
    return when (priority) {
        0 -> "Высокий"
        1 -> "Средний"
        2 -> "Низкий"
        else -> "Другой"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupScreen(
    groupId: Int,
    groupName: String,
    onBack: () -> Unit,
    onNavigateToChat: (Int) -> Unit,
    highlightItemId: Int? = null
) {
    val context = LocalContext.current
    val authManager = AuthManager(context)
    val viewModel = remember { GroupViewModel(authManager, groupId) }
    val uiState by viewModel.uiState

    val clipboardManager = LocalClipboardManager.current

    var showSearchDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<Item?>(null) }
    var showBuyDialog by remember { mutableStateOf(false) }
    var selectedItemId by remember { mutableStateOf<Int?>(null) }
    var itemId by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var priority by remember { mutableStateOf(1) }
    var budget by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var buyQuantity by remember { mutableStateOf("") }
    var showInviteCodeDialog by remember { mutableStateOf(false) }

    // Инициализация канала уведомлений
    LaunchedEffect(Unit) {
        NotificationHelper.createNotificationChannel(context)
    }

    // Запрос разрешения на уведомления
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Разрешение получено
        } else {
            Toast.makeText(context, "Разрешение на уведомления отклонено", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(showInviteCodeDialog) {
        if (showInviteCodeDialog) {
            viewModel.fetchInviteCode()
        }
    }

    if (showInviteCodeDialog) {
        AlertDialog(
            onDismissRequest = { showInviteCodeDialog = false },
            title = { Text("Инвайт-код группы") },
            text = {
                Column {
                    if (uiState.inviteCode != null) {
                        Text(
                            text = uiState.inviteCode!!,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .padding(bottom = 16.dp)
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(uiState.inviteCode!!))
                                    Toast.makeText(
                                        context,
                                        "Инвайт-код скопирован в буфер обмена",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        )
                    } else if (uiState.isLoadingInviteCode) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else if (uiState.errorMessage != null) {
                        Text(
                            text = uiState.errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showInviteCodeDialog = false }) {
                    Text("Закрыть")
                }
            },
            dismissButton = {}
        )
    }

    if (showSearchDialog) {
        AlertDialog(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.9f)
                .padding(4.dp),
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
                        .padding(8.dp)
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
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                Text(
                                    text = "Товары не найдены",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight(align = Alignment.CenterVertically),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    fontSize = 16.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentPadding = PaddingValues(vertical = 16.dp)
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
                .fillMaxWidth(0.91f)
                .fillMaxHeight(0.67f),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = {
                showDetailsDialog = false
                selectedItem = null
                itemId = ""
                quantity = "1"
                priority = 1
                budget = ""
            },
            title = {
                Text(
                    text = if (selectedItem != null) "Добавить ${selectedItem?.name}" else "Добавить товар вручную",
                    style = MaterialTheme.typography.titleMedium
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

                    Text(
                        text = "Приоритет",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Column {
                        listOf(
                            0 to "Высокий",
                            1 to "Средний",
                            2 to "Низкий"
                        ).forEach { (value, label) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.dp)
                            ) {
                                RadioButton(
                                    selected = priority == value,
                                    onClick = { priority = value }
                                )
                                Text(
                                    text = label,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = budget,
                        onValueChange = { budget = it },
                        label = { Text("Цена") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

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
                            priority = 1
                            budget = ""
                        }) { Text("Отмена") }
                        Button(
                            onClick = {
                                val quantityInt = quantity.toIntOrNull()
                                val priorityInt = priority
                                var budgetInt: Int? = budget.toIntOrNull()
                                if (budgetInt == null && selectedItem != null && quantityInt != null) {
                                    budgetInt = selectedItem!!.price * quantityInt
                                }
                                if (quantityInt != null) {
                                    if (selectedItem != null) {
                                        viewModel.addItem(selectedItem!!.id, quantityInt, priorityInt, budgetInt)
                                        // Отправляем уведомление только если разрешение есть
                                        NotificationHelper.sendNotification(
                                            context,
                                            "Товар добавлен",
                                            "Добавлен товар: ${selectedItem!!.name}, количество: $quantityInt",
                                            selectedItem!!.id
                                        )
                                    } else {
                                        val itemIdInt = itemId.toIntOrNull()
                                        if (itemIdInt != null) {
                                            viewModel.addItem(itemIdInt, quantityInt, priorityInt, budgetInt)
                                            NotificationHelper.sendNotification(
                                                context,
                                                "Товар добавлен",
                                                "Добавлен товар с ID: $itemIdInt, количество: $quantityInt",
                                                itemIdInt
                                            )
                                        }
                                    }
                                    showDetailsDialog = false
                                    selectedItem = null
                                    itemId = ""
                                    quantity = "1"
                                    priority = 1
                                    budget = ""
                                }
                            },
                            enabled = (selectedItem != null || itemId.toIntOrNull() != null) &&
                                    quantity.toIntOrNull() != null
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
                price = selectedItem.budget.toString()
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
                            // Отправляем уведомление
                            NotificationHelper.sendNotification(
                                context,
                                "Товар куплен",
                                "Куплен товар: ${selectedItem?.itemName}, количество: $quantityInt",
                                selectedItemId!!
                            )
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
                title = { Text("Группа $groupName") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { showInviteCodeDialog = true }) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_manage),
                            contentDescription = "Показать инвайт-код",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
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
                    Icon(
                        painterResource(id = R.drawable.chat_64d),
                        contentDescription = "Чат группы",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                FloatingActionButton(
                    onClick = { showSearchDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Добавить товар",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
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
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = uiState.items.sortedWith(
                            compareBy(
                                { it.priority },
                                { it.itemName }
                            )
                        )
                    ) { item ->
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
            .padding(1.dp),
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
            Column(
                modifier = Modifier.weight(1f)
            ) {
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
            .padding(vertical = 8.dp)
            .heightIn(min = 120.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 12.dp,
            pressedElevation = 16.dp
        ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
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
                    text = "Приоритет: ${priorityToCategory(item.priority)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                item.budget?.let {
                    Text(
                        text = "Цена: $it руб.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Button(
                onClick = onBuy,
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
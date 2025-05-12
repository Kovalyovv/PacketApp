package com.example.packetapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.packetapp.R
import com.example.packetapp.data.AuthManager
import com.example.packetapp.models.ChatMessage
import com.example.packetapp.utils.DateUtils
import com.example.packetapp.viewmodel.ChatViewModel
import org.joda.time.DateTime
import com.example.packetapp.models.MessageStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    groupId: Int,
    onBack: () -> Unit
) {
    val authManager = AuthManager(LocalContext.current)
    val chatViewModel = remember { ChatViewModel(authManager = authManager) }
    val messages by chatViewModel.messages.collectAsState()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val currentUserId = authManager.getUserId() ?: -1
    var messageText by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<ChatMessage?>(null) }
    val messageStatuses = remember { mutableStateMapOf<Int, MessageStatus>() }
    // Используем rememberSaveable для сохранения значения между рекомпозициями
    var tempMessageIdCounter by rememberSaveable { mutableStateOf(-1) }

    // Логирование обновления messages
    LaunchedEffect(messages) {
        println("ChatScreen: Messages updated, new count: ${messages.size}, messages: $messages")
        if (messages.isNotEmpty()) {
            scope.launch {
                println("ChatScreen: Scrolling to last message at index ${messages.size - 1}")
                listState.scrollToItem(messages.size - 1)
            }
        }
    }

    // Загрузка истории и подключение к WebSocket
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                println("ChatScreen: Connecting to chat for groupId $groupId")
                chatViewModel.connectToChat(groupId)
                // Прокручиваем к последнему сообщению после загрузки
                if (messages.isNotEmpty()) {
                    println("ChatScreen: Initial scroll to last message at index ${messages.size - 1}")
                    listState.scrollToItem(messages.size - 1)
                }
            } catch (e: Exception) {
                println("ChatScreen: Failed to connect to chat: ${e.message}")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Чат группы") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp)
            ) {
                replyingTo?.let { replyMessage ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ответ на: ${replyMessage.text}",
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(onClick = { replyingTo = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Отменить ответ")
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(12.dp),
                        textStyle = TextStyle(fontSize = 16.sp),
                        decorationBox = { innerTextField ->
                            if (messageText.isEmpty()) {
                                Text(
                                    text = "Введите сообщение...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    fontSize = 16.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                val timestamp = DateTime.now().toString()
                                // Генерируем уникальный временный id
                                val tempId = tempMessageIdCounter
                                tempMessageIdCounter -= 1 // Уменьшаем счётчик для следующего сообщения
                                val message = ChatMessage(
                                    id = tempId, // Уникальный временный id
                                    groupId = groupId,
                                    senderId = currentUserId,
                                    text = messageText,
                                    timestamp = timestamp,
                                    replyToId = replyingTo?.id
                                )
                                scope.launch {
                                    try {
                                        messageStatuses[tempId] = MessageStatus.SENDING
                                        // Убрали chatViewModel.updateMessages(message), так как это уже делается в ChatViewModel
                                        chatViewModel.sendMessage(message, replyingTo?.id)
                                        messageStatuses[tempId] = MessageStatus.SENT
                                        messageText = ""
                                        replyingTo = null
                                        println("ChatScreen: Message sent successfully with tempId: $tempId")
                                    } catch (e: Exception) {
                                        messageStatuses[tempId] = MessageStatus.FAILED
                                        println("ChatScreen: Failed to send message: ${e.message}")
                                    }
                                }
                            }
                        },
                        enabled = messageText.isNotBlank()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Отправить")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            state = listState
        ) {
            var lastDate: String? = null
            items(messages, key = { it.id }) { message ->
                val messageDate = DateUtils.formatDateTime(message.timestamp).split(" | ")[0] // "5 мая 2025"
                if (lastDate != null && lastDate != messageDate) {
                    // Разделитель даты
                    Text(
                        text = messageDate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .padding(4.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ChatMessageItem(
                    message = message,
                    messages = messages,
                    isCurrentUser = message.senderId == currentUserId,
                    status = messageStatuses[message.id] ?: MessageStatus.SENT,
                    onReply = { replyingTo = it },
                    onDelete = {
                        scope.launch {
                            try {
                                chatViewModel.deleteMessage(message.id)
                            } catch (e: Exception) {
                                println("ChatScreen: Failed to delete message: ${e.message}")
                            }
                        }
                    }
                )
                lastDate = messageDate
            }
        }
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    messages: List<ChatMessage>,
    isCurrentUser: Boolean,
    status: MessageStatus,
    onReply: (ChatMessage) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        val messageContent = @Composable {
            Column(
                modifier = Modifier
                    .wrapContentWidth()
                    .widthIn(max = (0.8f * LocalConfiguration.current.screenWidthDp).dp)
                    .background(
                        if (isCurrentUser) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp)
            ) {
                // Отображение сообщения, на которое отвечаем
                message.replyToId?.let { replyToId ->
                    val repliedMessage = messages.find { it.id == replyToId }
                    repliedMessage?.let {
                        Column(
                            modifier = Modifier
                                .wrapContentWidth()
                                .widthIn(max = (0.8f * LocalConfiguration.current.screenWidthDp).dp)
                                .background(
                                    if (isCurrentUser) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(4.dp)
                        ) {
                            Text(
                                text = "Пользователь ${it.senderId}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrentUser) MaterialTheme.colorScheme.onSecondaryContainer
                                else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = it.text,
                                fontSize = 12.sp,
                                color = if (isCurrentUser) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
                // Имя пользователя (только для чужих сообщений)
                if (!isCurrentUser) {
                    Text(
                        text = "Пользователь ${message.senderId}", // Заменить на реальное имя
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = message.text,
                    fontSize = 16.sp,
                    color = if (isCurrentUser) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = DateUtils.formatDateTime(message.timestamp).split(" | ")[1], // "02:27"
                        fontSize = 12.sp,
                        color = if (isCurrentUser) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    if (isCurrentUser) {
                        when (status) {
                            MessageStatus.SENDING -> CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 2.dp
                            )
                            MessageStatus.SENT -> Text("✓", fontSize = 12.sp, color = Color.Gray)
                            MessageStatus.READ -> Text("✓✓", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            MessageStatus.FAILED -> Text("!", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        if (!isCurrentUser) {
            messageContent()
        } else {
            messageContent()
        }

        Spacer(modifier = Modifier.width(8.dp))
        Column {
            IconButton(onClick = { onReply(message) }) {
                Icon(painterResource(id = R.drawable.reply), contentDescription = "Ответить")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить")
            }
        }
    }
}
package com.example.packetapp.ui.screens

import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.packetapp.R
import com.example.packetapp.data.AuthManager
import com.example.packetapp.models.ChatMessage
import com.example.packetapp.utils.DateUtils
import com.example.packetapp.viewmodel.ChatViewModel
import org.joda.time.DateTime
import com.example.packetapp.models.MessageStatus
import com.example.packetapp.network.ApiService
import com.example.packetapp.network.KtorClient
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    groupId: Int,
    onBack: () -> Unit
) {
    val authManager = AuthManager(LocalContext.current)
    val apiService = KtorClient.apiService
    val chatViewModel = remember { ChatViewModel(authManager = authManager, apiService = apiService) }
    val messages by chatViewModel.messages.collectAsState()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val currentUserId = authManager.getUserId() ?: -1
    var messageText by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<ChatMessage?>(null) }
    val messageStatuses = remember { mutableStateMapOf<String, MessageStatus>() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val context = LocalContext.current
    var isKeyboardVisible by remember { mutableStateOf(false) }
    var offsetY by remember { mutableStateOf(0.dp) }
    val animatedOffsetY by animateDpAsState(
        targetValue = offsetY,
        animationSpec = tween(durationMillis = 200)
    )


    val view = androidx.compose.ui.platform.LocalView.current
    DisposableEffect(view) {
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            val insets = ViewCompat.getRootWindowInsets(view)
            val imeHeight = insets?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
            isKeyboardVisible = imeHeight > 0
            offsetY = if (isKeyboardVisible) 50.dp else 0.dp
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(listener)
        onDispose {
            view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }

    LaunchedEffect(messages) {
        println("ChatScreen: Messages updated, new count: ${messages.size}, messages: $messages")
        if (messages.isNotEmpty()) {
            scope.launch {
                println("ChatScreen: Scrolling to last message at index ${messages.size - 1}")
                listState.scrollToItem(messages.size - 1)
            }
        }
    }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                println("ChatScreen: Connecting to chat for groupId $groupId")
                chatViewModel.connectToChat(groupId)
                if (messages.isNotEmpty()) {
                    println("ChatScreen: Initial scroll to last message at index ${messages.size - 1}")
                    listState.scrollToItem(messages.size - 1)
                }
            } catch (e: Exception) {
                println("ChatScreen: Failed to connect to chat: ${e.message}")
            }
        }
    }
    LaunchedEffect(groupId) {
        scope.launch {
            try {
                println("ChatScreen: Connecting to chat for groupId $groupId")
                chatViewModel.connectToChat(groupId)
                chatViewModel.preloadUserNames(groupId)
            } catch (e: Exception) {
                println("ChatScreen: Failed to connect to chat or preload names: ${e.message}")
            }
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Чат группы",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .offset(y = -animatedOffsetY) // Анимированное смещение вверх
                    .padding(horizontal = 12.dp, vertical = 2.dp)
                // Убираем imePadding()
            ) {
                replyingTo?.let { replyMessage ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ответ на: ${replyMessage.text}",
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 8.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(onClick = { replyingTo = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Отменить ответ")
                        }
                    }
                    Spacer(modifier = Modifier.height(1.dp))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = messageText,
                        onValueChange = {
                            messageText = it
                            if (messageText.isEmpty()) {
                                offsetY = 0.dp
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                keyboardController?.show()
                            },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        decorationBox = { innerTextField ->
                            Box(
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (messageText.isEmpty()) {
                                    Text(
                                        text = "Введите сообщение...",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                val timestamp = DateTime.now().toString()
                                val token = UUID.randomUUID().toString()
                                val message = ChatMessage(
                                    token = token,
                                    groupId = groupId,
                                    senderId = currentUserId,
                                    text = messageText,
                                    timestamp = timestamp,
                                    replyToToken = replyingTo?.token
                                )
                                scope.launch {
                                    try {
                                        messageStatuses[token] = MessageStatus.SENDING
                                        chatViewModel.sendMessage(message, replyingTo?.token)
                                        messageStatuses[token] = MessageStatus.SENT
                                        messageText = ""
                                        replyingTo = null
                                        offsetY = 0.dp
                                        println("ChatScreen: Message sent successfully with token: $token")
                                    } catch (e: Exception) {
                                        messageStatuses[token] = MessageStatus.FAILED
                                        println("ChatScreen: Failed to send message: ${e.message}")
                                    }
                                }
                            }
                        },
                        enabled = messageText.isNotBlank(),
                        modifier = Modifier
                            .background(
                                if (messageText.isNotBlank())
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(14.dp)
                            )
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Отправить",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            state = listState,
            verticalArrangement = Arrangement.Bottom,
            contentPadding = PaddingValues(bottom = if (isKeyboardVisible) 46.dp else 16.dp)
        ) {
            var lastDate: String? = null
            items(messages, key = { it.token }) { message ->
                val messageDate = DateUtils.formatDateTime(message.timestamp).split(" | ")[0]
                if (lastDate != null && lastDate != messageDate) {
                    Text(
                        text = messageDate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(vertical = 6.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
                ChatMessageItem(
                    message = message,
                    messages = messages,
                    isCurrentUser = message.senderId == currentUserId,
                    status = messageStatuses[message.token] ?: MessageStatus.SENT,
                    onReply = { replyingTo = it },
                    onDelete = {
                        scope.launch {
                            try {
                                chatViewModel.deleteMessage(message.token)
                            } catch (e: Exception) {
                                println("ChatScreen: Failed to delete message: ${e.message}")
                            }
                        }
                    },
                    chatViewModel = chatViewModel
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
    onDelete: () -> Unit,
    chatViewModel: ChatViewModel // Передаем ViewModel
) {
    val userName by produceState(initialValue = "", key1 = message.senderId) {
        value = chatViewModel.getUserName(message.senderId)
    }

    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 300),
        label = "MessageFadeIn"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .alpha(alpha),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val messageContent = @Composable {
            val timeText = DateUtils.formatDateTime(message.timestamp).split(" | ")[1]
            val timeWidth = with(LocalDensity.current) {
                val fontSizePx = 10.sp.toPx()
                val textWidth = fontSizePx * timeText.length * 0.1f
                textWidth + (if (isCurrentUser) 16.dp.toPx() else 0f)
            }.dp + 4.dp

            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .wrapContentHeight()
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .background(
                        if (isCurrentUser) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(start = 8.dp, top = 6.dp, end = 8.dp, bottom = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .wrapContentWidth()
                        .wrapContentHeight()
                        .padding(bottom = 14.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    message.replyToToken?.let { replyToToken ->
                        val repliedMessage = messages.find { it.token == replyToToken }
                        repliedMessage?.let {
                            val repliedUserName by produceState(initialValue = "Загрузка...", key1 = it.senderId) {
                                value = chatViewModel.getUserName(it.senderId)
                            }
                            Column(
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .background(
                                        if (isCurrentUser) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                        else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = repliedUserName,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                )
                                Text(
                                    text = it.text,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(1.dp))
                        }
                    }
                    if (!isCurrentUser) {
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Row(
                        modifier = Modifier
                            .wrapContentWidth()
                            .padding(end = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = message.text,
                            modifier = Modifier
                                .wrapContentWidth(),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 16.sp,
                                color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            textAlign = TextAlign.Start
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    if (isCurrentUser) {
                        when (status) {
                            MessageStatus.SENDING -> CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            MessageStatus.SENT -> Text(
                                "✓",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            )
                            MessageStatus.READ -> Text(
                                "✓✓",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            MessageStatus.FAILED -> Text(
                                "!",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            )
                        }
                    }
                }
            }
        }

        messageContent()

        Spacer(modifier = Modifier.width(8.dp))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(
                onClick = { onReply(message) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    painterResource(id = R.drawable.reply),
                    contentDescription = "Ответить",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (isCurrentUser) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
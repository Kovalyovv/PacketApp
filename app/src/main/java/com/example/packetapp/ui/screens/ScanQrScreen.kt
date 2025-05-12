package com.example.packetapp.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import com.example.packetapp.ui.viewmodel.ReceiptViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanQrScreen(
    onQrCodeScanned: (String) -> Unit,
    onBack: () -> Unit,
    context: Context,
    navigateToReceipt: (String) -> Unit, // Изменили сигнатуру на передачу qrCode
    receiptViewModel: ReceiptViewModel
) {
    val uiState by receiptViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var lastScannedQrCode by remember { mutableStateOf<String?>(null) } // Сохраняем последний отсканированный QR-код

    LaunchedEffect(uiState.checkData) {
        if (uiState.checkData != null && lastScannedQrCode != null) {
            navigateToReceipt(lastScannedQrCode!!) // Переходим с использованием QR-кода
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Сканировать QR-код") },
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                uiState.message?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                Text(
                    text = "Нажмите кнопку ниже, чтобы отправить тестовый QR-код.",
                    modifier = Modifier.padding(bottom = 16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Button(
                    onClick = {
                        val testQrCode = "t=20250427T1657&s=1372.38&fn=7380440700178304&i=10727&fp=3757497564&n=1"
                        lastScannedQrCode = testQrCode // Сохраняем QR-код
                        println("Using test QR code: $testQrCode")
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Отправка тестового QR-кода...")
                            delay(500L)
                            val result = receiptViewModel.processQrCode(testQrCode)
                            result.onSuccess {
                                snackbarHostState.showSnackbar("QR-код успешно обработан!")
                                receiptViewModel.loadReceiptData(testQrCode)
                                onQrCodeScanned(testQrCode)
                            }.onFailure { e ->
                                snackbarHostState.showSnackbar("Ошибка: ${e.message ?: "Неизвестная ошибка при обработке QR-кода"}")
                            }
                        }
                    },
                    enabled = uiState.message != "Этот QR-код уже был отсканирован другим пользователем"
                ) {
                    Text("Отправить тестовый QR-код")
                }

                uiState.errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}
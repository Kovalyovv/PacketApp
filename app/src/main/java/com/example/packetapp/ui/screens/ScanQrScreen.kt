package com.example.packetapp.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.packetapp.ui.viewmodel.ReceiptViewModel
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanQrScreen(
    onQrCodeScanned: (String) -> Unit,
    onBack: () -> Unit,
    context: Context,
    navigateToReceipt: (String) -> Unit,
    receiptViewModel: ReceiptViewModel
) {
    val uiState by receiptViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var lastScannedQrCode by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Запрос разрешения на камеру
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasCameraPermission = isGranted
            if (!isGranted) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Требуется разрешение на использование камеры")
                }
            }
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Переход на экран чека после успешной обработки
    LaunchedEffect(uiState.checkData) {
        if (uiState.checkData != null && lastScannedQrCode != null) {
            navigateToReceipt(lastScannedQrCode!!)
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
            if (hasCameraPermission) {
                CameraPreview(
                    onQrCodeDetected = { qrCode ->
                        if (lastScannedQrCode == null && !isProcessing) {
                            lastScannedQrCode = qrCode
                            isProcessing = true
                            coroutineScope.launch {
                                try {
                                    val result = receiptViewModel.processQrCode(qrCode)
                                    result.onSuccess {
                                        receiptViewModel.loadReceiptData(qrCode)
                                        onQrCodeScanned(qrCode)
                                    }.onFailure { e ->
                                        snackbarHostState.showSnackbar("Ошибка: ${e.message ?: "Неизвестная ошибка"}")
                                        lastScannedQrCode = null
                                    }
                                } finally {
                                    isProcessing = false
                                }
                            }
                        }
                    }
                )

                // Анимация ожидания
                if (isProcessing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        // Анимированный индикатор загрузки
                        val infiniteTransition = rememberInfiniteTransition()
                        val rotation by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            )
                        )

                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(60.dp)
                                .align(Alignment.Center)
                                .rotate(rotation),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 6.dp
                        )
                        Text(
                            text = "Обработка QR-кода...",
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(y = 80.dp)
                        )
                    }
                }

                // Оверлей для выделения области сканирования
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                        .background(Color.Transparent)
                )
            } else {
                Text(
                    text = "Требуется разрешение на использование камеры",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error
                )
            }

            uiState.errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreview(
    onQrCodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).also {
                previewView = it
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(view.surfaceProvider)
            }

            // Настройка сканирования QR-кодов
            val barcodeScanner = BarcodeScanning.getClient(
                BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build()
            )
            val executor = Executors.newSingleThreadExecutor()
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(executor) { imageProxy ->
                val mediaImage = imageProxy.image
                if (mediaImage != null) {
                    val image =
                        InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    barcodeScanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            barcodes.firstOrNull()?.rawValue?.let { qrCode ->
                                onQrCodeDetected(qrCode)
                            }
                        }
                        .addOnFailureListener { e ->
                            e.printStackTrace()
                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                        }
                }
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    )
}
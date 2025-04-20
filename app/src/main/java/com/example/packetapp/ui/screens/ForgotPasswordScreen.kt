package com.example.packetapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.packetapp.ui.viewmodel.ForgotPasswordViewModel

@Composable
fun ForgotPasswordScreen(
    onCodeSent: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val viewModel: ForgotPasswordViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onCodeSent()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Восстановление пароля",
            fontSize = 24.sp,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(16.dp))

        var email by remember { mutableStateOf("") }
        OutlinedTextField(
            value = email,
            onValueChange = { email = it.trim() }, // Обрезаем пробелы
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = uiState.errorMessage != null
        )

        uiState.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                color = Color.Red,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.requestResetCode(email) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Отправить код")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onNavigateToLogin) {
            Text("Вернуться к входу", color = Color(0xFF2196F3))
        }
    }
}
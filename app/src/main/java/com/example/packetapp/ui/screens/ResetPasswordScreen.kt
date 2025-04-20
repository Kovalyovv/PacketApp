package com.example.packetapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.packetapp.ui.viewmodel.ResetPasswordUiState
import com.example.packetapp.ui.viewmodel.ResetPasswordViewModel
import com.example.packetapp.ui.viewmodel.ResetPasswordViewModelFactory

@Composable
fun ResetPasswordScreen(
    onPasswordReset: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val viewModel: ResetPasswordViewModel = viewModel(
        factory = ResetPasswordViewModelFactory()
    )
    val uiState by viewModel.uiState

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            println("ResetPasswordScreen: Navigating to LoginScreen due to isSuccess")
            onPasswordReset()
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
            text = "Сброс пароля",
            fontSize = 24.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        var code by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }

        OutlinedTextField(
            value = code,
            onValueChange = { code = it },
            label = { Text("Код восстановления") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = { Text("Новый пароль") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                viewModel.resetPassword(code, newPassword, onPasswordReset)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Сбросить пароль")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onNavigateToLogin
        ) {
            Text("Вернуться к входу")
        }

        uiState.errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
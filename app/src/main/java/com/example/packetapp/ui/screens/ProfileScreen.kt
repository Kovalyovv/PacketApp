package com.example.packetapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.packetapp.data.AuthManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onNavigateToReceiptHistory: () -> Unit // Новый callback для навигации
) {
    val authManager = AuthManager(LocalContext.current)
    val userEmail = authManager.getUserEmail() ?: "Неизвестно"
    val userId = authManager.getUserId() ?: -1
    val name = authManager.getUserName() ?: userId.toString()

    // Пока нет API для получения имени пользователя, используем заглушку
    var userName by remember { mutableStateOf(name) }

    // Поля для изменения данных
    var newName by remember { mutableStateOf(userName) }
    var newEmail by remember { mutableStateOf(userEmail) }
    var newPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Профиль") }
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
            Text(
                text = "Информация о пользователе",
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Отображение текущих данных
            Text("Имя: $userName", fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Email: $userEmail", fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))

            // Поля для изменения данных
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Новое имя") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = newEmail,
                onValueChange = { newEmail = it },
                label = { Text("Новая почта") },
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

            // Кнопка для сохранения изменений
            Button(
                onClick = {
                    errorMessage = "Функциональность смены данных пока не реализована"
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сохранить изменения")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Новая кнопка для истории чеков
            Button(
                onClick = onNavigateToReceiptHistory,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("История чеков")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Кнопка выхода
            Button(
                onClick = {
                    authManager.clearAuthData()
                    onLogout()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Выйти из аккаунта")
            }

            errorMessage?.let {
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
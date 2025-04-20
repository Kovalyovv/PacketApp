package com.example.packetapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MainScreen(onLogout: () -> Unit) {
    val hasLoggedOut = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        println("MainScreen mounted")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Главный экран",
            fontSize = 24.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                println("Logout button clicked")
                hasLoggedOut.value = true
                onLogout()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Выйти")
        }
    }
}
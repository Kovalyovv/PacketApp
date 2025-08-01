//package com.example.packetapp.ui.screens
//
//import androidx.compose.foundation.layout.*
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.ArrowBack
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.unit.dp
//import com.example.packetapp.data.AuthManager
//import com.example.packetapp.ui.viewmodel.JoinGroupViewModel
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun JoinGroupScreen(
//    onJoinSuccess: () -> Unit,
//    onBack: () -> Unit
//) {
//    val authManager = AuthManager(LocalContext.current)
//    val viewModel = remember { JoinGroupViewModel(authManager) }
//    val uiState by viewModel.uiState
//
//    var inviteCode by remember { mutableStateOf("") }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Присоединиться к группе") },
//                navigationIcon = {
//                    IconButton(onClick = onBack) {
//                        Icon(
//                            imageVector = Icons.Default.ArrowBack,
//                            contentDescription = "Назад"
//                        )
//                    }
//                }
//            )
//        }
//    ) { padding ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(padding)
//                .padding(16.dp),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            OutlinedTextField(
//                value = inviteCode,
//                onValueChange = { inviteCode = it },
//                label = { Text("Инвайт-код") },
//                modifier = Modifier.fillMaxWidth()
//            )
//            Spacer(modifier = Modifier.height(16.dp))
//            Button(
//                onClick = { viewModel.joinGroup(inviteCode) },
//                enabled = !uiState.isLoading
//            ) {
//                Text("Присоединиться")
//            }
//            uiState.errorMessage?.let {
//                Spacer(modifier = Modifier.height(8.dp))
//                Text(
//                    text = it,
//                    color = MaterialTheme.colorScheme.error
//                )
//            }
//
//            if (uiState.isLoading) {
//                Spacer(modifier = Modifier.height(16.dp))
//                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
//            }
//        }
//    }
//
//    LaunchedEffect(uiState.isSuccess, uiState.errorStatus) {
//        if (uiState.isSuccess && uiState.errorStatus == null) {
//            onJoinSuccess()
//            viewModel.clearState()
//        }
//    }
//}
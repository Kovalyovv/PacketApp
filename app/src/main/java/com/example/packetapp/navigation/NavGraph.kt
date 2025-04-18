package com.example.packetapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.packetapp.ui.screens.LoginScreen
import com.example.packetapp.ui.screens.RegisterScreen

@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onLoginSuccess = { navController.navigate("main") },
                onNavigateToRegister = { navController.navigate("register") },
                onNavigateToForgotPassword = { navController.navigate("forgot_password") }
            )
        }
        composable("register") {
            RegisterScreen(
                onRegisterSuccess = { navController.navigate("main") },
                onNavigateToLogin = { navController.navigate("login") }
            )
        }
        composable("forgot_password") {
            // Экран восстановления пароля (добавим позже)
        }
        composable("main") {
            // Главный экран (добавим позже)
        }
    }
}
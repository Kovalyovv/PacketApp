package com.example.packetapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.packetapp.data.AuthManager
import com.example.packetapp.network.KtorClient
import com.example.packetapp.ui.screens.ForgotPasswordScreen
import com.example.packetapp.ui.screens.GroupScreen
import com.example.packetapp.ui.screens.LoginScreen
import com.example.packetapp.ui.screens.MainScreen
import com.example.packetapp.ui.screens.RegisterScreen
import com.example.packetapp.ui.screens.ResetPasswordScreen
import com.example.packetapp.ui.theme.PacketAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val authManager = AuthManager(this)
        KtorClient.initialize(authManager)

        setContent {
            PacketAppTheme {
                val startDestination = remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    val destination = withContext(Dispatchers.IO) {
                        if (authManager.isUserLoggedIn()) {
                            println("User has tokens, validating token")
                            val accessToken = authManager.getAccessToken()
                            try {
                                KtorClient.apiService.getUserProfile(accessToken!!)
                                println("Token is valid, starting at main")
                                "main"
                            } catch (e: Exception) {
                                println("Token validation failed: ${e.message}")
                                if (e.message == "Токен недействителен") {
                                    val refreshToken = authManager.getRefreshToken()
                                    try {
                                        val response = KtorClient.apiService.refreshToken(refreshToken!!)
                                        authManager.saveAuthData(
                                            response.token,
                                            response.refreshToken,
                                            response.user.id
                                        )
                                        println("Token refreshed, starting at main")
                                        "main"
                                    } catch (refreshError: Exception) {
                                        println("Refresh token failed: ${refreshError.message}")
                                        authManager.clearAuthData()
                                        "login"
                                    }
                                } else {
                                    authManager.clearAuthData()
                                    "login"
                                }
                            }
                        } else {
                            println("No tokens found, starting at login")
                            "login"
                        }
                    }
                    startDestination.value = destination
                }

                if (startDestination.value != null) {
                    AppNavGraph(startDestination = startDestination.value!!)
                } else {
                    Text(AnnotatedString("Загрузка..."))
                }
            }
        }
    }
}

@Composable
fun AppNavGraph(startDestination: String) {
    val navController = rememberNavController()

    println("AppNavGraph started with startDestination: $startDestination")
    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            println("Navigating to LoginScreen")
            LoginScreen(
                onLoginSuccess = {
                    println("onLoginSuccess called, navigating to main")
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToRegister = {
                    println("Navigating to RegisterScreen from LoginScreen")
                    navController.navigate("register") {
                        launchSingleTop = true
                    }
                },
                onNavigateToForgotPassword = {
                    println("Navigating to ForgotPasswordScreen from LoginScreen")
                    navController.navigate("forgotPassword") {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable("register") {
            println("Navigating to RegisterScreen")
            RegisterScreen(
                onRegisterSuccess = {
                    println("onRegisterSuccess called, navigating to main")
                    navController.navigate("main") {
                        popUpTo("register") { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToLogin = {
                    println("Navigating to LoginScreen from RegisterScreen")
                    navController.navigate("login") {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable("forgotPassword") {
            println("Navigating to ForgotPasswordScreen")
            ForgotPasswordScreen(
                onCodeSent = {
                    println("Navigating to ResetPasswordScreen from ForgotPasswordScreen")
                    navController.navigate("resetPassword") {
                        launchSingleTop = true
                    }
                },
                onNavigateToLogin = {
                    println("Navigating to LoginScreen from ForgotPasswordScreen")
                    navController.navigate("login") {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable("resetPassword") {
            println("Navigating to ResetPasswordScreen")
            ResetPasswordScreen(
                onPasswordReset = {
                    println("onPasswordReset called, navigating to login")
                    navController.navigate("login") {
                        popUpTo("resetPassword") { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToLogin = {
                    println("Navigating to LoginScreen from ResetPasswordScreen")
                    navController.navigate("login") {
                        popUpTo("resetPassword") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable("main") {
            println("Navigating to MainScreen")
            MainScreen(
                onLogout = {
                    println("onLogout called in AppNavGraph")
                    AuthManager(navController.context).clearAuthData()
                    navController.navigate("login") {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
                onNavigateToGroup = { groupId, highlightItemId ->
                    println("Navigating to GroupScreen with groupId: $groupId, highlightItemId: $highlightItemId")
                    val route = if (highlightItemId != null) {
                        "group/$groupId/$highlightItemId"
                    } else {
                        "group/$groupId"
                    }
                    navController.navigate(route)
                }
            )
        }
        composable(
            route = "group/{groupId}",
            arguments = listOf(navArgument("groupId") { type = NavType.IntType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getInt("groupId") ?: 0
            println("Navigating to GroupScreen with groupId: $groupId")
            GroupScreen(
                groupId = groupId,
                onBack = {
                    println("onBack called in GroupScreen, navigating back")
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "group/{groupId}/{highlightItemId}?",
            arguments = listOf(
                navArgument("groupId") { type = NavType.IntType },
                navArgument("highlightItemId") {
                    type = NavType.StringType // Изменяем тип на StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getInt("groupId") ?: 0
            val highlightItemId = backStackEntry.arguments?.getString("highlightItemId")?.toIntOrNull()
            println("Navigating to GroupScreen with groupId: $groupId, highlightItemId: $highlightItemId")
            GroupScreen(
                groupId = groupId,
                onBack = {
                    println("onBack called in GroupScreen, navigating back")
                    navController.popBackStack()
                },
                highlightItemId = highlightItemId
            )
        }
    }
}
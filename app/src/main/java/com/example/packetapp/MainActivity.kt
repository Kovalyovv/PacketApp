package com.example.packetapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.materialIcon
import com.example.packetapp.R
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.packetapp.data.AuthManager
import com.example.packetapp.network.KtorClient
import com.example.packetapp.ui.screens.ForgotPasswordScreen
import com.example.packetapp.ui.screens.GroupScreen
import com.example.packetapp.ui.screens.GroupsScreen
import com.example.packetapp.ui.screens.LoginScreen
import com.example.packetapp.ui.screens.MainScreen
import com.example.packetapp.ui.screens.PersonalListScreen
import com.example.packetapp.ui.screens.ProfileScreen
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
                                            response.user.id,
                                            response.user.name,
                                            response.user.email
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

    // Определяем элементы нижней навигации
    val navItems = listOf(
        NavItem("main", "Главная", { rememberVectorPainter(Icons.Default.Home) }),
        NavItem("personal_list", "Список", { rememberVectorPainter(Icons.Default.List) }),
        NavItem("groups", "Группы", { rememberVectorPainter(Icons.Default.Person) }),
        NavItem("qr", "QR", { painterResource(id = R.drawable.qr_code_icon32) }),
        NavItem("profile", "Профиль", { rememberVectorPainter(Icons.Default.AccountCircle) })
    )

    // Проверяем, находится ли текущий экран в списке экранов с нижней навигацией
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val showBottomBar = navItems.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    navItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon(),contentDescription = item.title) },
                            label = { Text(item.title) },
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
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
            composable("personal_list") {
                println("Navigating to PersonalListScreen")
                PersonalListScreen()
            }
            composable("groups") {
                println("Navigating to GroupsScreen")
                GroupsScreen(
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
            composable("profile") {
                println("Navigating to ProfileScreen")
                ProfileScreen(
                    onLogout = {
                        println("onLogout called in ProfileScreen")
                        AuthManager(navController.context).clearAuthData()
                        navController.navigate("login") {
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
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
                        type = NavType.StringType
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
}

// Перенесём NavItem сюда, так как он используется в AppNavGraph


data class NavItem(
    val route: String,
    val title: String,
    val icon: @Composable () -> Painter // Теперь иконка — это Composable-функция, возвращающая Painter
)
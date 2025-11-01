package com.proyecto.ganapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.proyecto.ganapp.ui.features.animals.AnimalsScreen
import com.proyecto.ganapp.ui.features.auth.LoginScreen
import com.proyecto.ganapp.ui.features.auth.RegisterScreen
import com.proyecto.ganapp.ui.features.notifications.NotificationsScreen
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")

    object Animals : Screen("animals/{userId}") {
        fun createRoute(userId: Long) = "animals/$userId"
    }
    object Notifications : Screen("notifications/{userId}") {
        fun createRoute(userId: Long) = "notifications/$userId"
    }
}

@Composable
fun NavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Screen.Login.route) {

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { navController.navigate(Screen.Home.route) },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = { navController.navigate(Screen.Login.route) },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable(Screen.Home.route) {
            Text("Bienvenido a GanApp 🐮", modifier = Modifier.padding(32.dp))
        }

        composable(Screen.Animals.route) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")?.toLongOrNull() ?: 0L
            AnimalsScreen(userId = userId)
        }

        composable(Screen.Notifications.route) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")?.toLongOrNull() ?: 0L
            NotificationsScreen(userId = userId)
        }

    }
}

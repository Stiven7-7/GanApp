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
import com.proyecto.ganapp.ui.features.animals.RegisterAnimalScreen
import com.proyecto.ganapp.ui.features.home.HomeScreen

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
                onLoginSuccess = { idUsuario ->
                    navController.navigate("home/$idUsuario")
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = { navController.navigate(Screen.Login.route) },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable("home/{idUsuario}") { backStackEntry ->
            val idUsuario = backStackEntry.arguments?.getString("idUsuario")?.toLong() ?: 0L
            HomeScreen(
                idUsuario = idUsuario,
                onRegistrarAnimal = { navController.navigate("register_animal/$idUsuario") },
                onVerAnimales = { navController.navigate("animals/$idUsuario") },
                onNotificaciones = { navController.navigate("notifications/$idUsuario") }
            )
        }

        composable("register_animal/{idUsuario}") { backStackEntry ->
            val idUsuario = backStackEntry.arguments?.getString("idUsuario")?.toLong() ?: 0L

            RegisterAnimalScreen(
                idUsuario = idUsuario,
                onAnimalSaved = {
                    navController.popBackStack() // vuelve al Home
                }
            )
        }

        composable("animals/{idUsuario}") { backStackEntry ->
            val idUsuario = backStackEntry.arguments?.getString("idUsuario")?.toLong() ?: 0L

            AnimalsScreen(
                userId = idUsuario,
                onAddAnimal = {
                    navController.navigate("register_animal/$idUsuario")
                }
            )
        }

        composable(Screen.Notifications.route) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")?.toLongOrNull() ?: 0L
            NotificationsScreen(userId = userId)
        }

    }
}

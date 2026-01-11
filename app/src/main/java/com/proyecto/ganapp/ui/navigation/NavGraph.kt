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
import com.proyecto.ganapp.ui.features.notifications.AnimalNotificationsScreen
import com.proyecto.ganapp.ui.features.notifications.AssignNotificationScreen
import com.proyecto.ganapp.ui.features.notifications.RegisterNotificationScreen

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

        // LOGIN
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

        // REGISTER USUARIO
        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = { navController.navigate(Screen.Login.route) },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        // HOME
        composable("home/{idUsuario}") { backStackEntry ->
            val idUsuario = backStackEntry.arguments?.getString("idUsuario")?.toLong() ?: 0L
            HomeScreen(
                idUsuario = idUsuario,
                onRegistrarAnimal = { navController.navigate("register_animal/$idUsuario") },
                onVerAnimales = { navController.navigate("animals/$idUsuario") },
                onNotificaciones = { navController.navigate("notifications/$idUsuario") }
            )
        }

        // REGISTRAR ANIMAL
        composable("register_animal/{idUsuario}") { backStackEntry ->
            val idUsuario = backStackEntry.arguments?.getString("idUsuario")?.toLong() ?: 0L

            RegisterAnimalScreen(
                idUsuario = idUsuario,
                onAnimalSaved = {
                    navController.popBackStack()
                }
            )
        }

        composable("animals/{userId}") { navBackStackEntry ->
            val userId = navBackStackEntry.arguments?.getString("userId")!!.toLong()

            AnimalsScreen(
                userId = userId,
                onAddAnimal = {
                    // quieres ir a registrar animal → usamos la ruta que ya existe
                    navController.navigate("register_animal/$userId")
                },
                onAssignNotification = { animalId ->
                    // pasamos userId y animalId
                    navController.navigate("assignNotification/$userId/$animalId")
                },
                onViewNotifications = { animalId ->
                    navController.navigate("animalNotifications/$animalId")
                }
            )
        }

        // ASIGNAR NOTIFICACIÓN A UN ANIMAL  ← AÑADE ESTO
        composable("assignNotification/{userId}/{animalId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")?.toLong() ?: 0L
            val animalId = backStackEntry.arguments?.getString("animalId")?.toLong() ?: 0L

            AssignNotificationScreen(
                userId = userId,
                animalId = animalId,
                onBack = { navController.popBackStack() }
            )
        }

        composable("animalNotifications/{animalId}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("animalId")!!.toLong()

            AnimalNotificationsScreen(
                animalId = id,
                onBack = { navController.popBackStack() }
            )
        }


        // LISTADO DE NOTIFICACIONES  ←❗ FALTABA ESTA
        composable("notifications/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")?.toLong() ?: 0L

            NotificationsScreen(
                userId = userId,
                onAddNotification = {
                    navController.navigate("register_notification/$userId")
                }
            )
        }

        // REGISTRAR NOTIFICACIÓN
        composable("register_notification/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")?.toLong() ?: 0L

            RegisterNotificationScreen(
                userId = userId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

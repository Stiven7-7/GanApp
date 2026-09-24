package com.proyecto.ganapp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.proyecto.ganapp.ui.features.animals.AnimalsScreen
import com.proyecto.ganapp.ui.features.animals.RegisterAnimalScreen
import com.proyecto.ganapp.ui.features.auth.LoginScreen
import com.proyecto.ganapp.ui.features.auth.RegisterScreen
import com.proyecto.ganapp.ui.features.home.HomeScreen
import com.proyecto.ganapp.ui.features.notifications.AnimalNotificationsScreen
import com.proyecto.ganapp.ui.features.notifications.AssignNotificationScreen
import com.proyecto.ganapp.ui.features.notifications.NotificationsScreen
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
fun RootNavigation(
    viewModel: RootSessionViewModel = hiltViewModel(),
) {
    val sessionState by viewModel.uiState.collectAsState()

    when (val state = sessionState) {
        RootSessionState.Loading -> RootLoadingContent()
        RootSessionState.Unauthenticated -> AuthNavGraph()
        is RootSessionState.Authenticated -> AppNavGraph(userId = state.userId)
    }
}

@Composable
private fun RootLoadingContent() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun AuthNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route,
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { _ ->
                    // No navegación imperativa.
                    // RootSessionState reaccionará al DataStore ya persistido.
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = { navController.navigate(Screen.Login.route) },
                onNavigateToLogin = { navController.popBackStack() },
            )
        }
    }
}

@Composable
private fun AppNavGraph(
    userId: Long,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                idUsuario = userId,
                onRegistrarAnimal = { navController.navigate("register_animal/$userId") },
                onVerAnimales = { navController.navigate("animals/$userId") },
                onNotificaciones = { navController.navigate("notifications/$userId") },
            )
        }

        composable("register_animal/{idUsuario}") { backStackEntry ->
            val idUsuario = backStackEntry.arguments?.getString("idUsuario")?.toLong() ?: 0L

            RegisterAnimalScreen(
                idUsuario = idUsuario,
                onAnimalSaved = {
                    navController.popBackStack()
                },
            )
        }

        composable("animals/{userId}") { navBackStackEntry ->
            val routeUserId = navBackStackEntry.arguments?.getString("userId")!!.toLong()

            AnimalsScreen(
                userId = routeUserId,
                onAddAnimal = {
                    navController.navigate("register_animal/$routeUserId")
                },
                onAssignNotification = { animalId ->
                    navController.navigate("assignNotification/$routeUserId/$animalId")
                },
                onViewNotifications = { animalId ->
                    navController.navigate("animalNotifications/$animalId")
                },
            )
        }

        composable("assignNotification/{userId}/{animalId}") { backStackEntry ->
            val routeUserId = backStackEntry.arguments?.getString("userId")?.toLong() ?: 0L
            val animalId = backStackEntry.arguments?.getString("animalId")?.toLong() ?: 0L

            AssignNotificationScreen(
                userId = routeUserId,
                animalId = animalId,
                onBack = { navController.popBackStack() },
            )
        }

        composable("animalNotifications/{animalId}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("animalId")!!.toLong()

            AnimalNotificationsScreen(
                animalId = id,
                onBack = { navController.popBackStack() },
            )
        }

        composable("notifications/{userId}") { backStackEntry ->
            val routeUserId = backStackEntry.arguments?.getString("userId")?.toLong() ?: 0L

            NotificationsScreen(
                userId = routeUserId,
                onAddNotification = {
                    navController.navigate("register_notification/$routeUserId")
                },
            )
        }

        composable("register_notification/{userId}") { backStackEntry ->
            val routeUserId = backStackEntry.arguments?.getString("userId")?.toLong() ?: 0L

            RegisterNotificationScreen(
                userId = routeUserId,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

package com.proyecto.ganapp.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.proyecto.ganapp.R
import com.proyecto.ganapp.ui.common.theme.GanAppSpacing
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
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.logo_app),
                contentDescription = "GANAPP",
                modifier = Modifier.size(104.dp),
            )
            Spacer(modifier = Modifier.height(GanAppSpacing.md))
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private const val AuthTransitionMillis = 220

@Composable
private fun AuthNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route,
    ) {
        composable(
            route = Screen.Login.route,
            exitTransition = {
                fadeOut(tween(AuthTransitionMillis)) +
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(AuthTransitionMillis),
                        targetOffset = { it / 12 },
                    )
            },
            popEnterTransition = {
                fadeIn(tween(AuthTransitionMillis)) +
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(AuthTransitionMillis),
                        initialOffset = { it / 12 },
                    )
            },
        ) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
            )
        }

        composable(
            route = Screen.Register.route,
            enterTransition = {
                fadeIn(tween(AuthTransitionMillis)) +
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(AuthTransitionMillis),
                        initialOffset = { it / 12 },
                    )
            },
            popExitTransition = {
                fadeOut(tween(AuthTransitionMillis)) +
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(AuthTransitionMillis),
                        targetOffset = { it / 12 },
                    )
            },
        ) {
            RegisterScreen(
                onRegisterSuccess = { navController.popBackStack() },
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

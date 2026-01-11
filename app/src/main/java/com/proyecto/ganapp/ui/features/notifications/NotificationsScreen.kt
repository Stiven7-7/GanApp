package com.proyecto.ganapp.ui.features.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.proyecto.ganapp.domain.model.Notificacion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    userId: Long,
    onAddNotification: () -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val notifications by viewModel.notifications.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadNotifications(userId)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Notificaciones") }) },

        floatingActionButton = {
            FloatingActionButton(onClick = onAddNotification) {
                Text("+")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            items(notifications) { noti ->
                NotificationCard(noti)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun NotificationCard(noti: Notificacion) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(noti.nombre, style = MaterialTheme.typography.titleMedium)
            Text("Tipo: ${noti.tipo}")
            Text("Inicio: ${noti.fechaInicio}")
            Text("Repetición: ${noti.seRepite}")
        }
    }
}


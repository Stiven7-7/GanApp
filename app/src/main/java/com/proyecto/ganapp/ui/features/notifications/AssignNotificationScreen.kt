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
fun AssignNotificationScreen(
    userId: Long,
    animalId: Long,
    onBack: () -> Unit,
    viewModel: AssignNotificationViewModel = hiltViewModel()
) {
    val notifications by viewModel.notifications.collectAsState()

    LaunchedEffect(userId) {
        viewModel.loadNotifications(userId)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Asignar notificación") }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {

            if (notifications.isEmpty()) {
                Text(
                    "No tienes notificaciones creadas aún.",
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn {
                    items(notifications) { noti ->
                        NotificationAssignCard(
                            noti = noti,
                            onAssign = {
                                viewModel.assign(animalId, noti.idNotificacion) {
                                    onBack()
                                }
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationAssignCard(
    noti: Notificacion,
    onAssign: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(noti.nombre, style = MaterialTheme.typography.titleMedium)
            Text("Tipo: ${noti.tipo}")
            Text("Repetición: ${noti.seRepite}")
            Spacer(Modifier.height(8.dp))
            Button(onClick = onAssign, modifier = Modifier.fillMaxWidth()) {
                Text("Asignar a este animal")
            }
        }
    }
}

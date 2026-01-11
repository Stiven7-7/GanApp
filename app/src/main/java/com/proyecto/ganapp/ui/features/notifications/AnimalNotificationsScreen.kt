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
fun AnimalNotificationsScreen(
    animalId: Long,
    onBack: () -> Unit,
    vm: AnimalNotificationsViewModel = hiltViewModel()
) {
    val list by vm.list.collectAsState()

    LaunchedEffect(Unit) {
        vm.load(animalId)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Notificaciones del animal") })
        }
    ) { padding ->

        if (list.isEmpty()) {
            Column(Modifier.padding(padding).padding(16.dp)) {
                Text("Este animal no tiene notificaciones asociadas.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).padding(16.dp)
            ) {
                items(list) { noti ->
                    NotificationItem(noti)
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun NotificationItem(noti: Notificacion) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(noti.nombre, style = MaterialTheme.typography.titleLarge)
            Text("Tipo: ${noti.tipo}")
            Text("Ciclo: ${noti.seRepite}")
            Text("Inicio: ${noti.fechaInicio}")
            Text("Fin: ${noti.fechaFin}")
            Text("Hora: ${noti.hora}")
        }
    }
}

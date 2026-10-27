package com.proyecto.ganapp.ui.features.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.hilt.navigation.compose.hiltViewModel
import com.proyecto.ganapp.ui.common.ganAppTopAppBarColors
import com.proyecto.ganapp.ui.common.theme.GanAppSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalNotificationsScreen(
    animalId: Long,
    onBack: () -> Unit,
    vm: AnimalNotificationsViewModel = hiltViewModel()
) {
    val list by vm.list.collectAsState()

    LaunchedEffect(animalId) {
        vm.load(animalId)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Notificaciones del animal") },
                expandedHeight = if (LocalDensity.current.fontScale >= 1.5f) {
                    GanAppSpacing.xxl * 3
                } else {
                    TopAppBarDefaults.TopAppBarExpandedHeight
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                        )
                    }
                },
                colors = ganAppTopAppBarColors(),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(GanAppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(GanAppSpacing.sm),
        ) {
            if (list.isEmpty()) {
                item {
                    NotificationEmptyState(
                        title = "No hay notificaciones asignadas",
                        message = "Este animal no tiene notificaciones asociadas.",
                    )
                }
            } else {
                items(list) { noti ->
                    NotificationVisualCard(
                        title = noti.nombre,
                        type = noti.tipo,
                        date = formatNotificationEpochDay(noti.fechaInicio),
                        time = noti.hora,
                        recurrence = noti.seRepite,
                        endDate = formatNotificationEpochDay(noti.fechaFin),
                    )
                }
            }
        }
    }
}

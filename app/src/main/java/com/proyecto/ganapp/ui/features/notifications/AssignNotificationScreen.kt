package com.proyecto.ganapp.ui.features.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.hilt.navigation.compose.hiltViewModel
import com.proyecto.ganapp.ui.common.theme.GanAppSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignNotificationScreen(
    userId: Long,
    animalId: Long,
    onBack: () -> Unit,
    viewModel: AssignNotificationViewModel = hiltViewModel()
) {
    val notifications by viewModel.notifications.collectAsState()
    val isAssigning by viewModel.isAssigning.collectAsState()

    LaunchedEffect(userId) {
        viewModel.loadNotifications(userId)
    }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            if (event is AssignNotificationEvent.Success) onBack()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Asignar notificación") },
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
            if (notifications.isEmpty()) {
                item {
                    NotificationEmptyState(
                        title = "No tienes notificaciones creadas aún.",
                    )
                }
            } else {
                items(notifications) { noti ->
                    NotificationVisualCard(
                        title = noti.nombre,
                        type = noti.tipo,
                        date = formatNotificationEpochDay(noti.fechaInicio),
                        time = noti.hora,
                        recurrence = noti.seRepite,
                        action = {
                            Button(
                                onClick = {
                                    viewModel.assign(
                                        userId = userId,
                                        animalId = animalId,
                                        notificationId = noti.idNotificacion,
                                    )
                                },
                                enabled = !isAssigning,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = GanAppSpacing.xxl),
                            ) {
                                Text("Asignar a este animal")
                            }
                        },
                    )
                }
            }
        }
    }
}

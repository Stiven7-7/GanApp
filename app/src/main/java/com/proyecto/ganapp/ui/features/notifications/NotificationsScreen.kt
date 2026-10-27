package com.proyecto.ganapp.ui.features.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.proyecto.ganapp.ui.common.ganAppTopAppBarColors
import com.proyecto.ganapp.ui.common.theme.GanAppSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    userId: Long,
    onAddNotification: () -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val notifications by viewModel.notifications.collectAsState()
    var hasNotificationPermission by remember {
        mutableStateOf(hasPostNotificationsPermission(context))
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationPermission = hasPostNotificationsPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(userId) {
        viewModel.loadNotifications(userId)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Notificaciones") },
                expandedHeight = if (LocalDensity.current.fontScale >= 1.5f) {
                    GanAppSpacing.xxl * 2
                } else {
                    TopAppBarDefaults.TopAppBarExpandedHeight
                },
                colors = ganAppTopAppBarColors(),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddNotification,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Crear notificación",
                )
            }
        },
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
        val narrow = maxWidth < 360.dp
        val highScale = LocalDensity.current.fontScale >= 1.5f
        val fabClearance = if (narrow) GanAppSpacing.xxl else 0.dp
        val bannerEndPadding = if (narrow && highScale) {
            GanAppSpacing.xxl + GanAppSpacing.md
        } else {
            0.dp
        }
        val buttonEndPadding = if (narrow && !highScale) fabClearance else 0.dp
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = GanAppSpacing.md,
                end = GanAppSpacing.md,
                top = GanAppSpacing.md,
                bottom = GanAppSpacing.xxl + GanAppSpacing.lg + GanAppSpacing.xs,
            ),
            verticalArrangement = Arrangement.spacedBy(GanAppSpacing.sm),
        ) {
            if (!hasNotificationPermission) {
                item {
                    NotificationPermissionBanner(
                        onActivate = {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        },
                        modifier = Modifier.padding(end = bannerEndPadding),
                        buttonEndPadding = buttonEndPadding,
                    )
                }
            }
            if (notifications.isEmpty()) {
                item {
                    NotificationEmptyState(
                        title = "No tienes notificaciones",
                        message = "Crea una notificación para comenzar.",
                        action = {
                            Button(
                                onClick = onAddNotification,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = fabClearance)
                                    .heightIn(min = GanAppSpacing.xxl),
                            ) {
                                Text("Crear notificación")
                            }
                        },
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
                    )
                }
            }
        }
        }
    }
}

private fun hasPostNotificationsPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return true
    }
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
}

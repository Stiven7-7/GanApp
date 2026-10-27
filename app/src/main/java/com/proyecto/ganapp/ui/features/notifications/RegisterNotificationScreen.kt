package com.proyecto.ganapp.ui.features.notifications

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.proyecto.ganapp.ui.common.ganAppTopAppBarColors
import com.proyecto.ganapp.ui.common.theme.GanAppSpacing
import java.time.LocalDate
import java.util.Calendar

private val notificationTypes = listOf("Vacunación", "Alimentación", "Ciclo Reproductivo")
private val notificationRepeats = listOf("NO", "DIARIA", "SEMANAL", "CADA 15 DÍAS", "MENSUAL", "ANUAL")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterNotificationScreen(
    userId: Long,
    onBack: () -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val nombre by viewModel.nombre.collectAsState()
    val tipo by viewModel.tipo.collectAsState()
    val fechaInicio by viewModel.fechaInicio.collectAsState()
    val fechaFin by viewModel.fechaFin.collectAsState()
    val seRepite by viewModel.seRepite.collectAsState()
    val hora by viewModel.hora.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            if (event is NotificationCreateEvent.Success) onBack()
        }
    }

    fun openDatePicker(onDateSelected: (Long) -> Unit) {
        val today = LocalDate.now()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                onDateSelected(selectedNotificationEpochDay(year, month + 1, dayOfMonth))
            },
            today.year,
            today.monthValue - 1,
            today.dayOfMonth,
        ).show()
    }

    fun openHourPicker(onHourSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        TimePickerDialog(
            context,
            { _, hour, minute ->
                onHourSelected(String.format("%02d:%02d", hour, minute))
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true,
        ).show()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Crear Notificación",
                        textAlign = TextAlign.Center,
                    )
                },
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
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(GanAppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(GanAppSpacing.lg),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(GanAppSpacing.sm)) {
                NotificationFormSectionTitle("Información")
                OutlinedNotificationTitle(
                    value = nombre,
                    onValueChange = { viewModel.nombre.value = it },
                )
                NotificationOptionMenu(
                    label = "Tipo",
                    selected = tipo,
                    placeholder = "Seleccionar tipo",
                    options = notificationTypes,
                    onSelected = { viewModel.tipo.value = it },
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(GanAppSpacing.sm)) {
                NotificationFormSectionTitle("Programación")
                NotificationScheduleField(
                    label = "Fecha inicio",
                    value = if (fechaInicio == 0L) "" else formatNotificationEpochDay(fechaInicio),
                    placeholder = "Seleccionar fecha inicio",
                    icon = Icons.Outlined.CalendarMonth,
                    onClick = { openDatePicker { viewModel.fechaInicio.value = it } },
                )
                NotificationScheduleField(
                    label = "Fecha fin",
                    value = if (fechaFin == 0L) "" else formatNotificationEpochDay(fechaFin),
                    placeholder = "Seleccionar fecha fin",
                    icon = Icons.Outlined.CalendarMonth,
                    onClick = { openDatePicker { viewModel.fechaFin.value = it } },
                )
                NotificationScheduleField(
                    label = "Hora",
                    value = hora,
                    placeholder = "",
                    icon = Icons.Outlined.Schedule,
                    onClick = { openHourPicker { viewModel.hora.value = it } },
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(GanAppSpacing.sm)) {
                NotificationFormSectionTitle("Repetición")
                NotificationOptionMenu(
                    label = "¿Se repite?",
                    selected = seRepite,
                    placeholder = "",
                    options = notificationRepeats,
                    onSelected = { viewModel.seRepite.value = it },
                )
            }

            Button(
                onClick = { viewModel.onSave(userId) },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(GanAppSpacing.lg),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.size(GanAppSpacing.xs))
                }
                Text(
                    text = "Guardar notificación",
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun OutlinedNotificationTitle(
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Título de la notificación") },
        placeholder = { Text("Ej: Vacuna aftosa") },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = GanAppSpacing.xxl),
        shape = MaterialTheme.shapes.medium,
    )
}

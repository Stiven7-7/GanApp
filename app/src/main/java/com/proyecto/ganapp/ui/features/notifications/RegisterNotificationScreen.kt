package com.proyecto.ganapp.ui.features.notifications

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.LocalDate
import java.util.Calendar

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

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            if (event is NotificationCreateEvent.Success) onBack()
        }
    }

    // Date Picker
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

    // Hour Picker
    fun openHourPicker(onHourSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        TimePickerDialog(
            context,
            { _, hour, minute ->
                onHourSelected(String.format("%02d:%02d", hour, minute))
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    fun formatDate(epochDay: Long): String {
        return formatNotificationEpochDay(epochDay)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Crear Notificación") }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(20.dp)
                .fillMaxWidth()
        ) {

            // Nombre
            OutlinedTextField(
                value = nombre,
                onValueChange = { viewModel.nombre.value = it },
                label = { Text("Título de la notificación") },
                placeholder = { Text("Ej: Vacuna aftosa") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // TIPO (Dropdown)
            TipoSelector(
                selected = tipo,
                onSelected = { viewModel.tipo.value = it }
            )

            Spacer(Modifier.height(16.dp))

            // Fecha Inicio
            OutlinedButton(
                onClick = { openDatePicker { viewModel.fechaInicio.value = it } },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2E7D32))
            ) {
                Text(if (fechaInicio == 0L) "Seleccionar fecha inicio" else formatDate(fechaInicio))
            }

            Spacer(Modifier.height(12.dp))

            // Fecha Fin
            OutlinedButton(
                onClick = { openDatePicker { viewModel.fechaFin.value = it } },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2E7D32))
            ) {
                Text(if (fechaFin == 0L) "Seleccionar fecha fin" else formatDate(fechaFin))
            }

            Spacer(Modifier.height(16.dp))

            // HORA
            OutlinedButton(
                onClick = { openHourPicker { viewModel.hora.value = it } },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2E7D32))
            ) {
                Text("Hora: $hora")
            }

            Spacer(Modifier.height(20.dp))

            // Repetición
            Text("¿Se repite?", style = MaterialTheme.typography.titleMedium)
            RepeatSelector(
                selected = seRepite,
                onSelected = { viewModel.seRepite.value = it }
            )

            Spacer(Modifier.height(30.dp))

            // GUARDAR
            Button(
                onClick = { viewModel.onSave(userId) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
            ) {
                Text("Guardar Notificación", color = Color.Black)
            }
        }
    }
}

/* ---------------------------- SELECTORES ---------------------------- */

@Composable
fun TipoSelector(
    selected: String,
    onSelected: (String) -> Unit
) {
    val opciones = listOf("Vacunación", "Alimentación", "Ciclo Reproductivo")
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2E7D32))
        ) {
            Text(selected.ifEmpty { "Seleccionar tipo" })
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            opciones.forEach {
                DropdownMenuItem(
                    text = { Text(it) },
                    onClick = {
                        onSelected(it)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun RepeatSelector(
    selected: String,
    onSelected: (String) -> Unit
) {
    val opciones = listOf("NO", "DIARIA", "SEMANAL", "CADA 15 DÍAS", "MENSUAL", "ANUAL")
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2E7D32))
        ) {
            Text(selected)
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            opciones.forEach {
                DropdownMenuItem(
                    text = { Text(it) },
                    onClick = {
                        onSelected(it)
                        expanded = false
                    }
                )
            }
        }
    }
}

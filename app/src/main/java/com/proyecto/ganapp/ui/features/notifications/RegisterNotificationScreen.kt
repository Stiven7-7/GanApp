package com.proyecto.ganapp.ui.features.notifications

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.proyecto.ganapp.domain.model.Notificacion
import java.text.SimpleDateFormat
import java.util.*

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

    LaunchedEffect(userId) {
        viewModel.idUsuario.value = userId
    }

    // EVENTOS
    LaunchedEffect(Unit) {
        viewModel.event.collect { evt ->
            if (evt == "SUCCESS") onBack()
        }
    }

    // Date Picker
    fun openDatePicker(onDateSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _: DatePicker, y: Int, m: Int, d: Int ->
                val c = Calendar.getInstance()
                c.set(y, m, d, 0, 0, 0)
                c.set(Calendar.MILLISECOND, 0)
                onDateSelected(c.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
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

    fun formatDate(ts: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date(ts))
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
                onClick = {
                    if (nombre.isBlank()) return@Button
                    if (fechaInicio == 0L || fechaFin == 0L) return@Button

                    val noti = Notificacion(
                        nombre = nombre,
                        tipo = tipo,
                        fechaInicio = fechaInicio,
                        fechaFin = fechaFin,
                        seRepite = seRepite,
                        hora = hora,
                        dosisPorDia = viewModel.dosisPorDia.value,
                        intervaloHoras = viewModel.intervaloHoras.value,
                        idUsuario = userId
                    )

                    viewModel.onSave(noti)
                },
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

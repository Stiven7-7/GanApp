package com.proyecto.ganapp.ui.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun HomeScreen(
    idUsuario: Long,
    onRegistrarAnimal: () -> Unit,
    onVerAnimales: () -> Unit,
    onNotificaciones: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val usuario by viewModel.usuario.collectAsState()

    LaunchedEffect(idUsuario) {
        viewModel.cargarUsuario(idUsuario)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF9FFF9)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Mi Finca",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0A0A0A)
            )

            Spacer(modifier = Modifier.height(8.dp))

            usuario?.let {
                Text(
                    text = "Bienvenido, ${it.nombre} 👋",
                    fontSize = 18.sp,
                    color = Color(0xFF4CAF50)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 🔘 Botón Registrar Animal
            Button(
                onClick = onRegistrarAnimal,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Text("Registrar Animal", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 🔘 Botón Ver Animales
            OutlinedButton(
                onClick = onVerAnimales,
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF000000)),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
            ) {
            Text("Ver Animales", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

            Spacer(modifier = Modifier.height(16.dp))

            // 🔘 Botón Notificaciones
            OutlinedButton(
                onClick = onVerAnimales,
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF000000)),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Text("Ver Animales", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
